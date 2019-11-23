# face plugin
This plugin contains the FaceRecognitionProcessor processor, which does face detection and recognition.

If no arguments are specified, the processor performs face detection, and generates a face similarity hash
for the first face found in the image.

If "known_faces" is provided, then this directory path is scanned for
images containing faces of known people, which are then used to label the asset accordingly when those faces are found.

The format of the file names within the known_faces folder should be like this:

[FIRST LAST].jpg

Multiple images can be provided for one person, by appending "+2", "+3" to the file name.