'use strict';

angular.module('exampleApp').controller('ExampleCtrl', ['$scope', function($scope) {
    $scope.testmodel = [];
    $scope.testdata = [
		{ id: 1, label: "David", disabled: true},
		{ id: 2, label: "Jhon"},
		{ id: 3, label: "Danny"}];
    $scope.testsettings = {
     showEnableSearchButton: true,
		 keyboardControls: true
    };

    $scope.testevents = {
      'onSelectionChanged': function () { // This event is not firing on selection of max limit
        alert("you changed selection");
      }
    }

    $scope.example1model = [];
	$scope.example1data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"}];


	$scope.example2model = [];
	$scope.example2data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"}];
	$scope.example2settings = {displayProp: 'id'};


	$scope.example3model = [];
	$scope.example3data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"},
		{id: 4, label: "Danny"}];
	$scope.example3settings = {displayProp: 'label', idProp: 'label'};

	$scope.example4model = [];
	$scope.example4data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"}];
	$scope.example4settings = {displayProp: 'label', idProp: 'id', externalIdProp: 'myCustomPropertyForTheObject'};

	$scope.example5model = [];
	$scope.example5data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"}];
	$scope.example5settings = {};
    $scope.example5customTexts = {buttonDefaultText: 'Select Users'};

	$scope.example6model = [{id: 1}, {id: 3}];
	$scope.example6data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"}];
	$scope.example6settings = {};

	$scope.example7model = [];
	$scope.example7data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"}];
	$scope.example7settings = { externalIdProp: '' };
	$scope.customFilter = 'a';

	$scope.example8model = [];
	$scope.example8data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"}];

	$scope.example9model = [];
	$scope.example9data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"}];
	$scope.example9settings = {enableSearch: true};

	$scope.example10model = [];
	$scope.example10data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"}];

	$scope.example10settings = {selectionLimit: 2};

	$scope.example12model = {};
	$scope.example12data = [
		{id: 1, label: "David"},
		{id: 2, label: "Jhon"},
		{id: 3, label: "Danny"}];

    $scope.example12settings = {selectionLimit: 1};


	$scope.example11model = [];
	$scope.example11data = [
		{id: 1, label: "David", gender: 'M'},
		{id: 2, label: "Jhon", gender: 'M'},
		{id: 3, label: "Lisa", gender: 'F'},
		{id: 4, label: "Nicole", gender: 'F'},
		{id: 5, label: "Danny", gender: 'M'}];

	$scope.example11settings = {
		groupByTextProvider: function(groupValue)
		{
			if (groupValue === 'M')
			{
				return 'Male';
			}
			else
			{
				return 'Female';
			}
		}
	};

	$scope.selectByGroupModel = [];
	$scope.selectByGroupData = [
		{ id: 1, label: "David", gender: 'M' },
		{ id: 2, label: "Jhon", gender: 'M' },
		{ id: 3, label: "Lisa", gender: 'F' },
		{ id: 4, label: "Nicole", gender: 'F' },
		{ id: 5, label: "Danny", gender: 'M' },
		{	id: 6, label: "Unknown", gender: 'O' }];

	$scope.selectByGroupSettings = {
		selectByGroups: ['F', 'M'],
		groupByTextProvider: function(groupValue) {
			switch (groupValue) {
				case 'M':
					return 'Male';
				case 'F':
					return 'Female';
				case 'O':
					return 'Other';
			}
		}
	};

    $scope.example13model = [];
    $scope.example13data = [
        {id: 1, label: "David"},
        {id: 2, label: "Jhon"},
        {id: 3, label: "Lisa"},
        {id: 4, label: "Nicole"},
        {id: 5, label: "Danny"}];

    $scope.example13settings = {
        smartButtonMaxItems: 3,
        smartButtonTextConverter: function(itemText, originalItem) {
            if (itemText === 'Jhon') {
                return 'Jhonny!';
            }

            return itemText;
        }
    };

    $scope.example14model = [];
    $scope.example14data = [
        {id: 1, label: "David"},
        {id: 2, label: "Jhon"},
        {id: 3, label: "Lisa"},
        {id: 4, label: "Nicole"},
        {id: 5, label: "Danny"},
        {id: 6, label: "Dan"},
        {id: 7, label: "Dean"},
        {id: 8, label: "Adam"},
        {id: 9, label: "Uri"},
        {id: 10, label: "Phil"}];

    $scope.example14settings = {
        scrollableHeight: '100px',
        scrollable: true
    };

    $scope.example15model = [];
    $scope.example15data = [
        {id: 1, label: "David"},
        {id: 2, label: "Jhon"},
        {id: 3, label: "Lisa"},
        {id: 4, label: "Nicole"},
        {id: 5, label: "Danny"}];

    $scope.example15settings = {
        enableSearch: true
    };

    $scope.example16model = [];
    $scope.example16data = [
        { id: 1, label: "David" },
        { id: 2, label: "Jhon" },
        { id: 3, label: "Lisa" },
        { id: 4, label: "Nicole" },
        { id: 5, label: "Danny" }];
    $scope.example16settings = {
    	styleActive: true
    };

    $scope.example17model = [];
    $scope.example17data = [
        { id: 1, label: "David" },
        { id: 2, label: "Jhon" },
        { id: 3, label: "Lisa" },
        { id: 4, label: "Nicole" },
        { id: 5, label: "Danny" }];
    $scope.example17settings = {
    	keyboardControls: true,
    };

    $scope.example18model = {};
    $scope.example18data = [
        { id: 1, label: "David" },
        { id: 2, label: "Jhon" },
        { id: 3, label: "Lisa" },
        { id: 4, label: "Nicole" },
        { id: 5, label: "Danny" }];
    $scope.example18settings = {
    	keyboardControls: true,
    	enableSearch: true,
    	selectionLimit: 1
    };

    $scope.example19model = {};
    $scope.example19data = [
        { id: 1, name: "David" },
        { id: 2, name: "Jhon" },
        { id: 3, name: "Lisa" },
        { id: 4, name: "Nicole" },
        { id: 5, name: "Danny" }];
    $scope.example19settings = {
    	template: '<b>{{option.name}}</b>'
    };

    $scope.example20model = [];
    $scope.example20data = [
			{ id: 1, label: "David", age: 23 },
			{ id: 2, label: "Jhon", age: 24 },
			{ id: 3, label: "Danny", age: 26 }];
    $scope.example20settings = {
    	searchField: 'age',
    	enableSearch: true
    };
		
	$scope.example21model = [];
    $scope.example21data = [
			{ id: 1, label: "David"},
			{ id: 2, label: "Jhon"},
			{ id: 3, label: "Danny"}];
    $scope.example21settings = {
    	showEnableSearchButton: true
    };

    $scope.searchSelectAllModel = [];
    $scope.searchSelectAllData = [
		{ id: 1, label: "David" },
		{ id: 2, label: "Jhon" },
		{ id: 3, label: "Danny" }
    ];
    $scope.searchSelectAllSettings = {
    	enableSearch: true,
		keyboardControls: true
    };
		
		$scope.disabledModel = [];
    $scope.disabledData = [
			{ id: 1, label: "David", disabled: true},
			{ id: 2, label: "Jhon"},
			{ id: 3, label: "Danny"}
		];
}]);
