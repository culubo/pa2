# pokemon battle ai agent

## course
cascs 440: artificial intelligence

## description

this project implements a tree traversal ai agent for pokemon battles. the agent uses monte carlo tree search (mcts) with uct (upper confidence bound for trees) to make optimal move decisions during pokemon battles. the agent evaluates possible moves by building a game tree, performing simulations, and selecting moves that maximize expected reward.

## project structure

```
pa2/
├── src/
│   └── pas/
│       └── pokemon/
│           └── agents/
│               └── TreeTraversalAgent.java    # template/starter code
├── 320396772/
│   ├── TreeTraversalAgent.java                # completed implementation
│   └── metadata.yml                           # submission metadata
├── lib/                                       # required jar libraries
│   ├── argparse4j-0.9.0.jar
│   ├── hamcrest-2.2.jar
│   ├── junit-4.12.jar
│   └── pokePA-0.0.1.jar                       # pokemon battle framework
├── doc/                                       # javadoc documentation
│   └── pas/pokemon/...
└── pokePA.pdf                                 # project documentation
```

## implementation details

the completed implementation in `320396772/TreeTraversalAgent.java` uses:

- **monte carlo tree search (mcts)**: builds a game tree by iteratively selecting, expanding, simulating, and backpropagating game states
- **uct algorithm**: uses upper confidence bound formula to balance exploitation (choosing good moves) and exploration (trying new moves)
- **game node evaluation**: evaluates terminal states and uses simulation rewards to guide move selection
- **move generation**: considers both battle moves and pokemon switching options
- **time management**: runs search in a background thread with timeout protection (6 minutes per move)

key constants:
- `C = sqrt(2)`: exploration constant for uct formula
- `MAX_DEPTH = 10`: maximum depth for simulations
- `NUM_SIMULATIONS = 100`: number of mcts iterations per move

## dependencies

the project requires the following libraries (included in `lib/`):
- `pokePA-0.0.1.jar`: pokemon battle framework providing core classes (Agent, Battle, BattleView, Team, Move, Pokemon, etc.)
- `argparse4j-0.9.0.jar`: command-line argument parsing
- `hamcrest-2.2.jar`: matcher library for testing
- `junit-4.12.jar`: unit testing framework

## build and run

### compilation

compile the source files with the required libraries on the classpath:

```bash
javac -cp "lib/*:." src/pas/pokemon/agents/TreeTraversalAgent.java
```

or for the completed implementation:

```bash
javac -cp "lib/*:." 320396772/TreeTraversalAgent.java
```

### running

the agent is designed to be used with the pokemon battle framework. refer to `pokePA.pdf` for specific instructions on how to run battles and configure the agent.

### key classes

the agent extends `edu.bu.pas.pokemon.core.Agent` and implements:
- `getMove(BattleView)`: selects the best move using mcts
- `chooseNextPokemon(BattleView)`: selects which pokemon to send out

the framework provides:
- `BattleView`: immutable view of the current battle state
- `MoveView`: view of available moves
- `TeamView`: view of pokemon team
- `PokemonView`: view of individual pokemon

## documentation

detailed javadoc documentation is available in the `doc/` directory. open `doc/pas/pokemon/index.html` in a browser to view the full api documentation.

## notes

- the `src/` directory contains the starter template code with todo markers
- the `320396772/` directory contains the completed implementation with mcts/uct algorithm
- the agent logs its activity to `agent.log` (if logging is enabled)
- the implementation uses concurrent execution to manage time limits per move

