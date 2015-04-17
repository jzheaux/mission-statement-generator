A fun program that rewrites itself to create evolving mission statements.

Technically, it is written in such a way that all the mutation, copy errors, 
etc. is sectioned off from the actual translation into a mission statement.
If anyone cares, one can separate the MissionStatementOutputStream and
InstructionSet initialization and replace them with some other fun
interpretation.

The class is carefully written to be completely self-contained. There are 
no jars and once a new "offspring" is created, it is completely independent
as well.

To run the first time, do the following:

> javac SelfContainedMissionStatement.java

Thereafter, there are a number of different options:

* Print Out Mission Statement:

> java SelfContainedMissionStatement

> Our mission is to evolve.

* Create New Mission Statement Based Off of Self-Copying:

> java SelfContainedMissionStatement Adam

> Our mission is to assimilate.

The following means "Using only SelfContainedMissionStatement, create 
Adam.java: A new mission statement that is capable of generating other
mission statements."

NOTE: These are based on random events. The first couple of runs, then could
produce mission statements that are identical to its predecessor. Evolution
takes time. :)

Now we can run

> java Adam

> Our mission is to assimilate.

And

> java SelfContainedMissionStatement Eve

> We will commit to evolve.

* Create New Mission Statement By Splicing DNA From Two Mission Statements

> java Adam Eve Seth

> It is our job to efficiently evolve.

Enjoy!
