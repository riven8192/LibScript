LibComponents
=============



## Simplest program
```
Hello World
```
Output:
```
Hello World
```


## Code blocks
```
BEGIN
	Hello World
END
```
```
Hello World
```


## Nested code blocks
```
BEGIN
	Hello World
	
	BEGIN
		Hi World
	END
	
	Bye World
END
```
```
Hello World
Hi World
Bye World
```

## Basic control flow (various loops)
```
Hello World


// with GOTO
BEGIN
	MY_LABEL:
	Hi World
	GOTO MY_LABEL     # jumps to label
END


// with LOOP
BEGIN
	Hi World
	LOOP     # jumps to beginning of current code block
END


// with IF ... LOOP
BEGIN
	Hi World
	IF <condition> LOOP
END


// with IF ... THEN
IF <condition> THEN
	Hi World
	LOOP
END


// with WHILE
WHILE <condition> DO
	Hi World
END


Bye World
```
```
Hello World
Hi World
Hi World
Hi World
Hi World
Hi World
...
Bye World
```


## Functions
```
FUNCTION sumfin
	Hello World
END

CALL sumfin
CALL sumfin
```
```
Hello World
Hello World
```

## Nested functions
```
FUNCTION sumfin
	Hello World
	
	FUNCTION aww
		Bye World
	END
	
	CALL aww
END

FUNCTION aww           # note function with same name, in different scope
	Odd World
END

CALL sumfin
CALL sumfin
CALL aww
```
```
Hello World
Bye World
Hello World
Bye World
Odd World
```


## Scheduling hints
##### (this crude language's *raison d'être*)
```
Hello World

YIELD              # interrupt and reschedule immediately
SLEEP <time>       # interrupt and reschedule after specific amount of time

Hi World

WAIT               # interrupt and reschedule manually

Bye World

HALT               # interrupt and halt the script (cannot be resumed)

Odd World
```

## Control flow
```
BREAK              # terminates current scope
LOOP               # go to beginning of current scope
GOTO <label>       # go to label in current scope
CALL <function>    # execute function in current scope or parent (recursively)
HALT               # terminates all open scopes
THROW <id>         # terminates open scopes until scope with found with CATCH <id>
```

## Conditional control flow & scheduling
```
IF <condition> BREAK
IF <condition> CALL <function>
IF NOT <condition> CALL <function>
IF NOT <condition> GOTO <label> ELSE SLEEP <time>
IF <condition> WAIT ELSE THROW 10001
```

## Conditional blocks
```
IF <condition> THEN
	...
END

WHILE NOT <condition> DO
	...
END
```

## Throwing and catching errors
```
FUNCTION a
	CALL b
END

FUNCTION b
	FUNCTION c
		Hello World
		THROW 10001
		Bye World
	END
	
	FUNCTION d
		Odd World
	END
	
	
	CALL c
	HALT                   # never reaches this line as function 'c' aborts
	
	CATCH 10001 CALL d
	CATCH 10002 CALL e     # never reaches this line as 10002 is never thrown
	
	Hi World
END

CALL a
```
```
Hello World
Odd World
Hi World
```

## Example program
```
SLEEP 3s


# let passengers leave
WHILE bus.hasOutboundPassenger() DO
	bus.makeOutboundPassengerLeaveBus()
	SLEEP 500ms
END


# let passengers enter
WHILE NOT bus.shouldDepart() DO
	IF bus.hasInboundPassenger() THEN
		bus.makeInboundPassengerEnterBus()
	END
	
	SLEEP 0.25s
END


SLEEP 2s


# travel!
bus.depart()
WAIT             # resume script when game determines bus has reached destination
bus.onArrive()


LOOP
```