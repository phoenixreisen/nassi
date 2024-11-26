## nassi

*nassi* is a tool, that allows you to generate Nassi-Shneiderman-like diagrams from textual representation.

Here at [Phoenix Reisen GmbH](https://www.phoenixreisen.com/) we mainly use *nassi* to create software specifications / Use Cases.

### Generating diagrams

Let's start with a simple [example](/examples/ex1.nassi): 

```
The alarm clock is ringing!

IF Today is a working day? {
  Turn off alarm clock.
  Slowly wake up.
  Get up
  Go to work.
}
ELSE {
  Turn off alarm clock.
  Continue sleeping...
}
```

BTW, if you're using Vim, you're lucky - there is a [Vim syntax file](extra/nassi.vim) for *nassi* source files.

In order to generate the diagram, we call *nassi* as follows (assuming we're in *nassi*s' the project root dir):

```
java -jar bin/nassi.jar -o ex1.html examples/ex1.nassi
```

This is what the resulting diagram looks like:

![Morning Routine 1](./examples/ex1.jpg "Morning Routine 1")


### Diffing

Ok, now let's refine our [morning routine](example/ex2.nassi) and make it more realistic:

```
The alarm clock is ringing!

IF Today is a working day? {
  WHILE As long as I still have 5 minutes left {
    Hit the snooze button
    Sleep a little further
  }
  Turn off alarm clock.
  Get up
  Haste to work.
}
ELSE {
  Turn off alarm clock.
  Continue sleeping... 
}
```

To highlight the changes between our original morning routine and the revised one, we can generate a side-by-side diff like so:

```
java -jar bin/nassi.jar -o diff.html --diff examples/ex1.nassi examples/ex2.nassi
```

And here is what the resulting diff looks like:

![Morning Routine Diff](./examples/diff_ex1_ex2.jpg "Morning Routine Diff")


### Pro Tip

While working on a spezification it's very useful to have the textual representation and the resulting diagram in sync, so that whenever you save the textual represenation in your text editor, the diagram gets generated automatically. You can achieve this using the [entr](https://github.com/eradman/entr) command:


```
echo examples/ex1.nassi | entr -p java -jar bin/nassi.jar -o examples/ex1.html /_
  
```


## Usage

The executable JAR is in the `bin` directory.

```
$ java -jar bin/nassi.jar
```


## Build the executable

If you want (for whatever reason) to build the executable (a stand-alone JAR file) yourself, you need to execute the following [Leiningen](https://leiningen.org/) commands:


```
$ lein clean; lein uberjar
```


## License

Copyright (c) 2024 Phoenix Reisen GmbH

BSD 3-Clause (see file LICENSE).
