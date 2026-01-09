package src.pas.pokemon.agents;

import edu.bu.pas.pokemon.core.Agent;
import edu.bu.pas.pokemon.core.Battle.BattleView;
import edu.bu.pas.pokemon.core.Move.MoveView;
import edu.bu.pas.pokemon.core.Team.TeamView;
import edu.bu.pas.pokemon.core.Pokemon.PokemonView;
import edu.bu.pas.pokemon.core.SwitchMove;
import edu.bu.pas.pokemon.utils.Pair;

import java.util.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.concurrent.*;

public class TreeTraversalAgent extends Agent {
    private static final double C = Math.sqrt(2);
    private static final int MAX_DEPTH = 10;
    private static final int NUM_SIMULATIONS = 100;
    private final long maxThinkingTimePerMoveInMS;
    private PrintWriter logWriter;

    public TreeTraversalAgent() {
        super();
        this.maxThinkingTimePerMoveInMS = 360000; // 6 min/move
        logWriter = null;
        try {
            logWriter = new PrintWriter(new FileWriter("agent.log", true));
        } catch (IOException e) {
            System.err.println("Could not create log file: " + e.getMessage());
        }
    }

    @Override
    public MoveView getMove(BattleView battleView) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        MoveView move = null;
        long durationInMs = 0;

        Future<Pair<MoveView, Long>> future = executor.submit(() -> {
            double startTime = System.nanoTime();
            MoveView bestMove = findBestMove(battleView, this.getMyTeamIdx());
            double endTime = System.nanoTime();
            return new Pair<>(bestMove, (long)((endTime - startTime) / 1000000));
        });

        try {
            Pair<MoveView, Long> result = future.get(this.maxThinkingTimePerMoveInMS, TimeUnit.MILLISECONDS);
            move = result.getFirst();
            durationInMs = result.getSecond();
        } catch (TimeoutException e) {
            System.err.println("Timeout!");
            System.err.println("Team [" + (this.getMyTeamIdx() + 1) + "] loses!");
            System.exit(-1);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.exit(-1);
        } finally {
            executor.shutdown();
        }

        return move;
    }

    @Override
    public Integer chooseNextPokemon(BattleView view) {
        TeamView myTeam = this.getMyTeamView(view);
        for (int idx = 0; idx < myTeam.size(); ++idx) {
            if (!myTeam.getPokemonView(idx).hasFainted()) {
                return idx;
            }
        }
        return null;
    }

    public MoveView findBestMove(BattleView rootView, int myTeamIdx) {
        GameNode root = new GameNode(rootView, myTeamIdx);
        
        for (int i = 0; i < NUM_SIMULATIONS; i++) {
            GameNode selectedNode = select(root);
            double reward = simulate(selectedNode);
            backpropagate(selectedNode, reward);
        }

        List<MoveView> possibleMoves = generatePossibleMoves(rootView, myTeamIdx);
        MoveView bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (MoveView move : possibleMoves) {
            BattleView newState = simulateMove(rootView, move);
            GameNode child = new GameNode(newState, myTeamIdx);
            // Combine simulation reward with move power heuristic
            double avgReward = (child.getVisits() > 0) ? (child.getValue() / child.getVisits()) : child.evaluate();
            Integer power = move.getPower();
            double powerBonus = (power != null && power > 0) ? power / 100.0 : 0.0; // Normalize power to 0-1 range
            double score = avgReward + powerBonus; // Bias toward higher-power moves
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private GameNode select(GameNode node) {
        while (!node.isTerminal()) {
            List<MoveView> possibleMoves = generatePossibleMoves(node.getState(), node.getTeamIdx());
            if (possibleMoves.isEmpty()) {
                break;
            }

            List<GameNode> children = new ArrayList<>();
            for (MoveView move : possibleMoves) {
                BattleView newState = simulateMove(node.getState(), move);
                GameNode child = new GameNode(newState, node.getTeamIdx());
                children.add(child);
            }

            GameNode selectedChild = null;
            double bestUCT = Double.NEGATIVE_INFINITY;

            for (GameNode child : children) {
                double exploitation = child.getValue();
                double exploration = Math.sqrt(Math.log(node.getVisits()) / (child.getVisits() + 1));
                double uct = exploitation + C * exploration;

                if (uct > bestUCT) {
                    bestUCT = uct;
                    selectedChild = child;
                }
            }

            if (selectedChild == null) {
                break;
            }
            node = selectedChild;
        }
        return node;
    }

    private double simulate(GameNode node) {
        if (node.isTerminal()) {
            return node.evaluate();
        }

        BattleView currentState = node.getState();
        int depth = 0;
        int currentTeamIdx = node.getTeamIdx();

        while (!currentState.isOver() && depth < MAX_DEPTH) {
            List<MoveView> possibleMoves = generatePossibleMoves(currentState, currentTeamIdx);
            if (possibleMoves.isEmpty()) {
                break;
            }

            MoveView randomMove = possibleMoves.get(new Random().nextInt(possibleMoves.size()));
            currentState = simulateMove(currentState, randomMove);
            currentTeamIdx = 1 - currentTeamIdx;
            depth++;
        }

        GameNode finalNode = new GameNode(currentState, node.getTeamIdx());
        return finalNode.evaluate();
    }

    private void backpropagate(GameNode node, double reward) {
        node.updateValue(reward);
    }

    private List<MoveView> generatePossibleMoves(BattleView state, int teamIdx) {
        List<MoveView> moves = new ArrayList<>();
        TeamView team = state.getTeamView(teamIdx);
        PokemonView activePokemon = team.getActivePokemonView();
        
        moves.addAll(activePokemon.getAvailableMoves());

        for (int i = 0; i < team.size(); i++) {
            if (!team.getPokemonView(i).hasFainted() && i != team.getActivePokemonIdx()) {
                moves.add(new SwitchMove(i).getView());
            }
        }

        return moves;
    }

    private BattleView simulateMove(BattleView state, MoveView move) {
        List<Pair<Double, BattleView>> preMoveStates = state.applyPreMoveConditions(0);
        if (preMoveStates.isEmpty()) {
            return state;
        }
        
        BattleView newState = preMoveStates.get(0).getSecond();
        List<BattleView> postTurnStates = newState.applyPostTurnConditions();
        if (postTurnStates.isEmpty()) {
            return newState;
        }
        
        return postTurnStates.get(0);
    }

    private class GameNode {
        private BattleView state;
        private int teamIdx;
        private double value;
        private int visits;

        public GameNode(BattleView state, int teamIdx) {
            this.state = state;
            this.teamIdx = teamIdx;
            this.value = 0;
            this.visits = 0;
        }

        public BattleView getState() { return state; }
        public int getTeamIdx() { return teamIdx; }
        public double getValue() { return value; }
        public int getVisits() { return visits; }

        public boolean isTerminal() {
            return state.isOver();
        }

        public double evaluate() {
            if (state.isOver()) {
                return state.getTeamView(teamIdx).getActivePokemonView().hasFainted() ? -1.0 : 1.0;
            }
            return 0.0;
        }

        public void updateValue(double newValue) {
            visits++;
            value += newValue;
        }
    }

    public void close() {
        if (logWriter != null) {
            logWriter.close();
        }
    }
}