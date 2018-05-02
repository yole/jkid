JKid is a simple JSON serialization/deserialization library for Kotlin data classes.
To serialize or deserialize an object use the 'serialize' and 'deserialize' functions.

This project accompanies the Chapter 10, "Introspecting Kotlin Code" of the [Kotlin in Action](https://www.manning.com/books/kotlin-in-action) book.

The section "Declaring and Applying Annotations" of the Chapter 10 describes annotations in Kotlin, and also shows how to use the library and how to tune it with annotations.
You can find examples for this section in the folder `test/kotlin/examples`.
Five examples correspond to five subsections accordingly.
The file `kotlin/Annotations.kt` contains the declarations of discussed annotations.

The section "Reflection: Introspecting Kotlin Objects at Runtime" of the book describes the implementation of serializer and deserializer.
The files `main/kotlin/serialization/Serializer.kt` and `main/kotlin/deserialization/Deserializer.kt` contain the source code.

We highly encourage you to do the following exercises after reading the text.
The first exercise can be started after reading the description of the serializer, to be exact after reading the section "Customizing Serialization with Annotations".
The second exercise is intended to be started after reading the whole chapter.
Solving these exercises will help you to understand the concepts better and lets you practice right away.

1. Support the annotation `DateFormat`, that allows to annotate the date property with `@DateFormat("dd-MM-yyyy")` specifying the date format as an argument.
The testing example showing its usage is in the file `test/kotlin/exercise/DateFormatAnnotation.kt`.
Remove `@Ignore` from the test `DateFormatTest` and make it pass.
The declaration of the annotation is in the file `main/kotlin/exercise/DateFormat.kt`.
The solution to this exercise can be found in the branch `solution-date-format`.
You can use the action "compare with Branch..." to compare your solution with the suggested one.

2. Make JKid support serialization and deserialization of maps as property values.
For now it supports only objects and collections.
The example is in the file `test/kotlin/exercise/Map.kt`.
Remove `@Ignore` from the test `MapTest` and make it pass.
To support deserialization of maps, create a class `MapSeed` similar to `ObjectSeed` and collection seeds.
The function `createSeedForType` should now return an instance of `MapSeed` if a map is expected.
The example solution can be found in the branch `solution-map`.
