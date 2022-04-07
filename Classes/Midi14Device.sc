AbstractMIDI14Device{
    var <initialized=false;
    var <cc14;
    var ccs, chans;

    *new{|connectOnInit=true, registerDefaultFunctions=true|
        ^super.new.init(connectOnInit, registerDefaultFunctions)
    }

    init{|connectOnInit, registerDefaultFunctions|
        chans = (0..15);
        ccs = (0..63);

        // Midi responders will be organized in this structure of arrays
        cc14 = chans.collect{|midiChan|
            // This is capped at 64 because a 14 bit midi signal takes up two cc's and so only 64 are available
            ccs.collect{|ccNum|
                (
                   responder: nil,
                    name: "cc%".format(ccNum).asSymbol,
                    normalization: \unipolar, // May be \unipolar, \bipolar or \raw
                )
            }
        };

        if(connectOnInit, {
			this.connect();
		});

        // if(registerDefaultFunctions, {
        //     this.setDefaults();
        // })
        this.setup();
        initialized = true;
    }

    // Device specific setup may be done here
    setup{
        "Setting up %".format(this.class).postln;
    }

    // Overwrite in subclasses
    controllerName{
        this.subclassResponsibility(thisMethod)
    }

    getCC14{|channel, ccnum|
        ^cc14[channel][ccnum]
    }

    disableAll{
        this.allCC14Do{|cc|
            cc.responder.disable()
        }
    }

    enableAll{
        this.allCC14Do{|cc|
            cc.responder.enable()
        }
    }

    allCC14Do{|func|
        cc14.do{|midiChan, chanNum|
            midiChan.do{|cc, ccNum|
                func.value(cc, ccNum, chanNum)
            }
        }
    }

    // TODO rewrite normalization to spec
    setFunc{|channel, ccnum, function, normalization|
        if(cc14[channel][ccnum].responder.isNil, {
            "%: Responder for channel %, cc % does not exist".format(this.class.name, channel, ccnum).error
        }, {
            cc14[channel][ccnum].function = function;
            cc14[channel][ccnum].responder.func_({|val, chan, cc1, cc2|
                // Perform normalization
                val = switch (normalization,
                    \unipolar, {
                        val.linlin(0, CC14.maxValue, 0.0, 1.0);
                    },
                    \bipolar, {
                        val.linlin(0, CC14.maxValue, -1.0, 1.0);
                    },
                    \raw, {
                        val
                    }
                );

                // Now call the user's responder function with the (potentially) scaled value.
                function.value(val, chan, cc1, cc2)
            })
        });
    }

    // Adds a cc14 responder and sets it's function
    addCC14{|name, channel, ccnum, function, normalization=\unipolar|
        var dict = cc14[channel][ccnum];

        if(dict.responder.notNil, {
            dict.responder.disable();
        });

        dict.channel = channel;
        dict.ccnum = ccnum;
        dict.function = function;

        dict.name = name ? dict[name];
        dict.normalization = normalization ? dict[normalization];

        dict.responder = CC14.new(
            cc1: ccnum,
            cc2: ccnum + 32,
            chan: channel,
            fix: true,
            normalizeValues: false // Handled by the function
        );

        this.setFunc(
            channel: channel,
            ccnum: ccnum,

            function: function,
            normalization: normalization
        );

        cc14[channel][ccnum] = dict;
    }

    // An overengineered way of connecting only this controller to SuperCollider. Much faster (at least on Linux) than connecting all.
    connect{

        // Used to hack MIDIIn to say whether our controller is connected or not
        var connectMethod = "is%Connected".format(this.controllerName).asSymbol;

        // Connect midi controller
        if(MIDIClient.initialized.not, {
            "MIDIClient not initialized... initializing now".postln;
            MIDIClient.init;
        });

        // This ratsnest connects only this controller and not all, which is much faster than the latter.
        MIDIClient.sources.do{|src, srcNum|
            if(src.device == this.controllerName.asString, {
                if(try{MIDIIn.connectMethod}.isNil, {
                    var isSource = MIDIClient.sources.any({|e|
                        e.device==this.controllerName.asString
                    });

                    if(isSource, {
                            "Connecting %".format(this.controllerName).postln;
                            MIDIIn
                            .connect(srcNum, src)
                            .addUniqueMethod(connectMethod, {
                                true
                            }
                        )
                    });
                }, {
                    "% is already connected... (device is busy)".format(
                        this.controllerName
                    ).warn
                });
            });
        };
    }

}
