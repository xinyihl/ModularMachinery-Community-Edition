package github.kasuminova.mmce.common.helper;


import crafttweaker.api.block.IBlockState;

import java.util.function.Predicate;

@FunctionalInterface
public interface IBlockStatePredicate extends Predicate<IBlockState> {

    @Override
    boolean test(IBlockState t);

}
