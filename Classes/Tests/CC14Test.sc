CC14Test1 : UnitTest {
	test_check_classname {
		var result = CC14.new;
		this.assert(result.class == CC14);
	}
}


CC14Tester {
	*new {
		^super.new.init();
	}

	init {
		CC14Test1.run;
	}
}
