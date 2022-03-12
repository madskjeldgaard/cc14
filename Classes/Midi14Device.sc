MIDI14Device{
    var <cc14;

    *new{|connectOnInit=true, registerDefaultFunctions=true|
        ^super.new.init(connectOnInit, registerDefaultFunctions)
    }

    init{|connectOnInit, registerDefaultFunctions|

        // Midi responders will be organized in this structure of arrays
        cc14 = (0..15).collect{|midiChan|
            // This is capped at 64 because a 14 bit midi signal takes up two cc's and so only 64 are available
            (0..63).collect{|ccNum|
                (
                    responder: nil,
                    name: "cc%".format(ccNum).asSymbol,
                    normalization: \uni, // May be \uni, \bi or \raw
                    ease: EaseNone, // May be any of Ease.allSubclasses
                )
            }
        };

        if(connectOnInit, {
			this.connect();
		});

        if(registerDefaultFunctions, {
            this.setDefaults();
        })
    }

    // Overwrite in subclass
    setDefaults{
        cc14.do{|midiChan, midiChanNum|
            midiChan.do{|cc}

        }
        addCC14
    }

    // Overwrite in subclasses
    controllerName{
        this.subclassResponsibility(thisMethod)
    }

    // A silly little thing that randomizes the easing of all functions
    // scrambleEase{
    //     cc14.do{|chan|
    //         chan.do{|cc|
    //             if(cc.responder.notNil, {
    //                 // Choose random ease function
    //                 cc.ease = Ease.allSubclasses.choose;
    //                 this.setFunc(cc.channel, cc.ccnum, cc.function, cc.ease, cc.normalization)
    //             })
    //         }
    //     }
    // }

    getCC14{|channel, ccnum|
        ^cc14[channel][ccnum]
    }

    setFunc{|channel, ccnum, function, ease, normalization|
        if(cc14[channel][ccnum].responder.isNil, {
            "%: Responder for channel %, cc % does not exist".format(this.class.name, channel, ccnum).error
        }, {
            cc14[channel][ccnum].function = function;
            // TODO: Ease and normalization not used yet. Needs to be enclosed in function like in CC14.func
            cc14[channel][ccnum].responder.func_({|val, chan, cc1, cc2|
                // Perform normalization
                val = switch (normalization,
                    \unipolar, {
                        var uni = val.linlin(0, CC14.maxValue, 0.0, 1.0);

                        // Perform easing function
                        ease.value(uni);
                    },
                    \bipolar, {
                        var uni = val.linlin(0, CC14.maxValue, 0.0, 1.0);

                        // Perform easing function
                        uni = ease.value(uni);

                        // Then scale to bipolar
                        // TODO: This is kind of waste of resources. Needs optimization.
                        uni.linlin(0.0,1.0,-1.0,1.0)
                    },
                    \raw, {
                        val
                    }
                );

                // Now call the user's responder function with the (potentially) scaled and eased value.
                function.value(val, chan, cc1, cc2)
            })
        });
    }

    // Adds a cc14 responder and sets it's function
    addCC14{|name, channel, ccnum, function, normalization=\uni, ease(EaseNone)|
        var dict = cc14[channel][ccnum];

        dict.channel = channel;
        dict.ccnum = ccnum;
        dict.function = function;

        dict.name = name ? dict[name];
        // TODO: Not used yet
        dict.normalization = normalization ? dict[normalization];
        // TODO: Not used yet
        dict.ease = ease ? dict[ease];

        dict.responder = CC14.new(
            cc1: ccnum,
            cc2: ccnum + 32,
            chan: channel,
            fix: true,
            normalizeValues: \raw // Handled by the function
        );

        this.setFunc(
            channel: channel,
            ccnum: ccnum,
            function: function,
            ease: ease,
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
