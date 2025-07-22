package archives.tater.savepoint.mixin.accessories;

//@SuppressWarnings("UnstableApiUsage")
//@Mixin(AccessoriesEventHandler.class)
//public class AccessoriesEventHandlerMixin {
//    @ModifyReturnValue(
//            method = "dropStack",
//            at = @At(value = "RETURN", ordinal = 1)
//    )
//    private static ItemStack keepItem(ItemStack original, @Local ItemStack stack) {
//        if (!SavePoint.accessoriesKept) return original;
//        SavePoint.accessoriesKept = false;
//        return stack;
//    }
//}
