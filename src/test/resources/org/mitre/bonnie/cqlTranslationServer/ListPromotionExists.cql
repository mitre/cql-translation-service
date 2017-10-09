library TestListPromotionExists

using QUICK

context Patient

define Foo: First({1, 2, 3, 4})
define Bar: exists Foo
