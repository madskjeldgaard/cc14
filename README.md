# CC14

### 14 bit midi functionality and helper functions

14 bit midi functionality and helper functions for the same. Some of this is builds on the original work of Carl Testa's great [FourteenBitCC class](https://gist.github.com/carltesta/bb5065a7b92bab7673237e9cc1c9a612) but with extended functionality and convenience.

See the help file for information about the functionality and use of this extension.

As an alternative to this quark, you could use the 14 bit midi functionality in [The Modality Toolkit](https://github.com/ModalityTeam/Modality-toolkit).

If you don't have a 14 bit midi controller, I recommend building one yourself (the easiest way would probably be to use the Teensy microcontrollers since they have great usb midi support).


### Contents

#### CC14
This class represents a responder for a 14 bit midi signal.

A 14 bit midi signal is comprised of two 7 bit midi cc signals. These two cc numbers are combined to create a 14 bit number which is why this class takes two CC numbers, as opposed to the singular CC number of a 7 bit responder.

This responder also features a couple of nice side effects: 

It is possible to register a single function in the responder or a collection of functions. The latter is useful if you want to easily create different responders for one 14 bit midi cc value, either using it for different things or creating different transformations of the incoming signal.

A built in pattern is available as well by calling the `.pattern` method.

Optionally, you can choose to transform the raw midi data to a normalized value in the range of 0.0 to 1.0.


### Installation

Open up SuperCollider and evaluate the following line of code:
`Quarks.install("https://github.com/madskjeldgaard/cc14")`
