package ishgard;

import java.util.List;
import java.util.ArrayList;

public class Recipe
{
    public final List<Material> mats;
    public final int numGems;

    public Recipe(Material one, Material two, Material three, int gems)
    {
        numGems = gems;
        mats = new ArrayList<>();
        mats.add(one);
        mats.add(two);
        mats.add(three);
    }

    public boolean usesMat(Material mat)
    {
        return mats.contains(mat);
    }
}