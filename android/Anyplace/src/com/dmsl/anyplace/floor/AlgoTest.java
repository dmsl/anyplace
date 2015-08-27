package com.dmsl.anyplace.floor;

import android.content.Context;

public class AlgoTest extends FloorSelector{

	public AlgoTest(Context myContext) {
		super(myContext);
	}

	@Override
	protected String calculateFloor(Args args) throws Exception {
		
		//throw new Exception("Server Error: ");
		return "2";
	}

}
