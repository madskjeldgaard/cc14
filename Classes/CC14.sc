// Based on Carl Testa's FourteenBitCC class
CC14 {
	var <cc1;
	var <cc2;
	var <chan;
	var <def;
	var msb;
	var lsb;
	var <value;
	var <pattern;
	var <maxValue;
	var normValues;

	var function;
	var functionKind;

	*new {arg cc1 , cc2 , chan, fix=true, normalizeValues=true;
		^super.new.init(cc1, cc2, chan, fix, normalizeValues)
	}

	init { arg aCc1, aCc2, aChan, fix, normalizeValues;
		normValues = normalizeValues;
		maxValue = 16383;

		cc1 = aCc1;
		cc2 = aCc2;
		chan = aChan;

		pattern = PatternProxy(0);
		def = MIDIFunc.cc({
			|val,num,chan,src|

			pattern.source = val;

			case
			{num==cc1}{this.msbSet(val)}
			{num==cc2}{this.lsbSet(val)}

		}, [cc1,cc2],chan);

		if(fix, { def.fix; });

	}
	
	msbSet { arg byte;
		msb = byte;
		this.check;
	}

	lsbSet { arg byte;
		lsb = byte;
		this.check;
	}

	check {
		if(lsb.notNil and: { msb.notNil }) {
			this.prCallFunction();
			msb = nil;
			lsb = nil;
		}
	}

	func_{|inFunction|
		function = case

		// A singular function
		{ inFunction.isKindOf(Function) } {
			functionKind = \function;
			inFunction
		} 

		// A collection of functions to evaluate
		{ inFunction.isKindOf(Collection) } {
			functionKind = \collection;
			inFunction
		}

		// Everything else is an error
		{ inFunction.isKindOf(Function).not and: { inFunction.isKindOf(Collection).not } } {
			"CC14 can only register a Function or a Collection of functions".error;
		};
	}

	prCallFunction{
		value = (msb << 7 + lsb);

		// Scale / Normalize
		if(normValues, { 
			value = value.linlin(
				0, 
				maxValue, 
				0.0, 
				1.0
			);
		});

		switch(functionKind, 
			\function, {
				function.value(value, chan, cc1, cc2);
			}, 
			\collection, {
				function.do{|funcInCollection|
					funcInCollection.value(value, chan, cc1, cc2)
				}
			}
		);
		
		pattern.source = value;
	}
}
