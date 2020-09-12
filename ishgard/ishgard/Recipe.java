package ishgard;

import java.util.List;
import java.util.ArrayList;

public class Recipe implements Comparable<Recipe>
{
    public List<Material> mats;
    public int numGems;

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

    @Override
    public int compareTo(Recipe o)
    {
        return numGems - o.numGems;
    }
}