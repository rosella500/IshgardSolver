package ishgard;

import static ishgard.Crafter.*;
import static ishgard.Material.*;

import java.util.Map;
import java.util.Scanner;
import java.util.EnumMap;

import java.util.Set; 
import java.util.HashSet; 


public class IshgardSolver
{
    
    protected static final int MAT_MULTIPLIER = 10;

    private static final int WASTED_MAT_WEIGHT = 20;
    private static final int GEM_WEIGHT = 1;
    private static final int SUBDIVISION = 200 / MAT_MULTIPLIER;

    protected static final Map<Crafter, Recipe> RECIPES = new EnumMap<>(Crafter.class);

    public static void main(String[] args)
    {
        RECIPES.put(CRP, new Recipe(Log, Wheat, Cotton, 8));
        RECIPES.put(BSM, new Recipe(Ore, Mudstone, Log, 0));
        RECIPES.put(ARM, new Recipe(Ore, Mudstone, Dust, 0));
        RECIPES.put(GSM, new Recipe(Mudstone, Dust, Resin, 0));
        RECIPES.put(LTW, new Recipe(Adder, Log, Cotton, 8));
        RECIPES.put(WVR, new Recipe(Cotton, Resin, Water, 0));
        RECIPES.put(ALC, new Recipe(Adder, Salt, Water, 4));
        RECIPES.put(CUL, new Recipe(Wheat, Water, Salt, 4));

        Map<Material, Integer> totalMats = new EnumMap<>(Material.class);
        Scanner sc = new Scanner(System.in);
        for(Material m : Material.values())
        {
            System.out.println("How many "+m+"s do you have?");
            totalMats.put(m, sc.nextInt() / MAT_MULTIPLIER);
        }
        sc.nextLine();

        int mult = -1;
        for(Map.Entry<Material, Integer> entry : totalMats.entrySet())
        {
            int tempMult = entry.getValue() / SUBDIVISION;

            if(mult <= 0 || (tempMult > 0 && tempMult < mult))
            {
                mult = tempMult;
            }
        }

        if(mult <= 0)
        {
            mult = 1;
        }

        Map<Material, Integer> workingMatMap = new EnumMap<>(Material.class);

        for(Map.Entry<Material,Integer> entry : totalMats.entrySet())
        {
            workingMatMap.put(entry.getKey(), entry.getValue() / mult);
        }
        

        Solution bestSolution = getBestSolution(workingMatMap);
        bestSolution.mult(mult);

        Map<Material,Integer> leftoverMats = bestSolution.getLeftoverMats(totalMats);
        System.out.println("Leftover mats: "+leftoverMats);
        Solution leftoverSolution = getBestSolution(leftoverMats);

        bestSolution.add(leftoverSolution);

        System.out.println(bestSolution.getSummary(totalMats));
        System.out.println("Press enter to exit...");

        sc.nextLine();
        sc.close();
    }


    public static Solution getBestSolution(Solution current, Crafter crafter, Map<Material, Integer> mats, Set<Integer> calculatedBranches, int accumulator)
    {
        if(!current.canFitCrafter(crafter, mats))
        {
            return null;
        }

        accumulator *= crafter.getValue();
        current.addCraft(crafter);
        if(!calculatedBranches.add(accumulator))
        { 
            //Already calculated
            return null;
        }
        boolean canContinue = false;
        for(Crafter craft : Crafter.values())
        {
            if(current.canFitCrafter(craft, mats))
            {
                canContinue = true;
                break;
            }
        }

        if(canContinue)
        {
            return getBestSolution(mats, getBestSolution(new Solution(current), CRP, mats, calculatedBranches, accumulator),
            getBestSolution(new Solution(current), BSM, mats, calculatedBranches, accumulator),
            getBestSolution(new Solution(current), ARM, mats, calculatedBranches, accumulator),
            getBestSolution(new Solution(current), GSM, mats, calculatedBranches, accumulator),
            getBestSolution(new Solution(current), LTW, mats, calculatedBranches, accumulator),
            getBestSolution(new Solution(current), WVR, mats, calculatedBranches, accumulator),
            getBestSolution(new Solution(current), ALC, mats, calculatedBranches, accumulator),
            getBestSolution(new Solution(current), CUL, mats, calculatedBranches, accumulator)); 
        }

        return current;
    }

    public static Solution getBestSolution(Map<Material,Integer> mats)
    {
        Set<Integer> calculatedBranches = new HashSet<>();
        int accumulator = 1;

        long firstTimestamp = System.currentTimeMillis();
        Solution crp = getBestSolution(new Solution(), CRP, mats, calculatedBranches, accumulator);
        Solution bsm = getBestSolution(new Solution(), BSM, mats, calculatedBranches, accumulator);
        Solution arm = getBestSolution(new Solution(), ARM, mats, calculatedBranches, accumulator);
        Solution gsm = getBestSolution(new Solution(), GSM, mats, calculatedBranches, accumulator);
        Solution ltw = getBestSolution(new Solution(), LTW, mats, calculatedBranches, accumulator);
        Solution wvr = getBestSolution(new Solution(), WVR, mats, calculatedBranches, accumulator);
        Solution alc = getBestSolution(new Solution(), ALC, mats, calculatedBranches, accumulator);
        Solution cul = getBestSolution(new Solution(), CUL, mats, calculatedBranches, accumulator);

        Solution bestSolution = getBestSolution(mats, crp, bsm, arm, gsm, ltw, wvr, alc, cul); 
        System.out.println("Took "+(System.currentTimeMillis() - firstTimestamp)+"ms and calculated results of "+calculatedBranches.size()+" combinations");
        return bestSolution;
    }

    public static int getGemsFromSolution(Solution s)
    {
        int gems = 0;
        for(Map.Entry<Crafter,Integer> entry : s.getCrafts().entrySet())
        {
            gems += RECIPES.get(entry.getKey()).numGems * (entry.getValue() == null ? 0 : entry.getValue());
        }
        return gems;
    }

    public static int getWastedMatsFromSolution(Solution s, Map<Material, Integer> matMap)
    {
        int mats = 0;
        for(Material mat : Material.values())
        {
            int numMatsInvolved = s.getMats().get(mat) == null? 0 : s.getMats().get(mat);
            
            mats += matMap.get(mat) - numMatsInvolved;
        }
        return mats;
    }

    public static Solution getBestSolution(Map<Material,Integer> mats, Solution... solutions)
    {
        Solution bestSolution = null;
        int bestWeightedValue = 0;

        for(Solution solution : solutions)
        {
            if(solution == null)
            {
                continue;
            }

            int currentGems = getGemsFromSolution(solution);
            int currentWastedMats = getWastedMatsFromSolution(solution, mats);

            int weightedValue = currentGems * GEM_WEIGHT / 4 + currentWastedMats * WASTED_MAT_WEIGHT;

            if(bestSolution == null || weightedValue < bestWeightedValue)
            {
                bestSolution = solution;
                bestWeightedValue = weightedValue;
            }
        }

        return bestSolution;
    }

    public static <K> void addValueToMap(Map<K,Integer> map, K key, Integer value)
    {
        if(map.containsKey(key))
        {
            map.put(key, map.get(key) + value);
        }
        else
        {
            map.put(key, value);
        }
    }

    private static class Solution 
    {
        Map<Crafter,Integer> crafts;
        Map<Material,Integer> matsUsed;

        public Solution(Solution toClone)
        {
            crafts = new EnumMap<>(toClone.getCrafts());
            matsUsed = new EnumMap<>(toClone.getMats());
        }
        public Solution()
        {
            crafts = new EnumMap<>(Crafter.class);
            matsUsed = new EnumMap<>(Material.class);
        }

        public boolean canFitCrafter(Crafter craft, Map<Material, Integer> mats)
        {
            for(Material m : RECIPES.get(craft).mats)
            {
                if((matsUsed.get(m) == null ? 0 : matsUsed.get(m)) + 1 > mats.get(m))
                {
                    return false;
                }
            }

            return true;
        }

        public void addCraft(Crafter craft)
        {
            addCraft(craft, 1);
        }

        private void addCraft(Crafter craft, int amount)
        {
            addValueToMap(crafts, craft, amount);
            for(Material m : RECIPES.get(craft).mats)
            {
                addValueToMap(matsUsed, m, amount);
            }
        }

        public Map<Crafter,Integer> getCrafts()
        {
            return crafts;
        }

        public Map<Material,Integer> getMats()
        {
            return matsUsed;
        }

        public Map<Material, Integer> getLeftoverMats(Map<Material, Integer> mats)
        {
            Map<Material, Integer> leftoverMats = new EnumMap<>(Material.class);
            for(Map.Entry<Material,Integer> mat : mats.entrySet())
            {
                leftoverMats.put(mat.getKey(), (mat.getValue() == null? 0 : mat.getValue()) - (matsUsed.get(mat.getKey()) == null? 0 : matsUsed.get(mat.getKey())));
            }

            return leftoverMats;
        }

        public int calcTotalCrafts()
        {
            int numCrafts = 0;
            for(Map.Entry<Crafter,Integer> entry : crafts.entrySet())
            {
                numCrafts += entry.getValue();
            }
            return numCrafts;
        }

        public void mult(int multiplier)
        {
            for(Map.Entry<Crafter,Integer> craft : crafts.entrySet())
            {
                crafts.put(craft.getKey(), craft.getValue() == null? 0 : craft.getValue() * multiplier);
            }

            for(Map.Entry<Material,Integer> mat : matsUsed.entrySet())
            {
                matsUsed.put(mat.getKey(), mat.getValue() == null? 0 : mat.getValue() * multiplier);
            }
        }

        public void add(Solution other)
        {
            if(other == null)
                return;

            for(Map.Entry<Crafter, Integer> craft : other.getCrafts().entrySet())
            {
                addCraft(craft.getKey(), craft.getValue());
            }
        }

        public String getSummary(Map<Material, Integer> totalMats)
        {
            StringBuilder toStr = new StringBuilder();
            for(Map.Entry<Crafter,Integer> entry : crafts.entrySet())
            {
                toStr.append(entry.getKey()).append("\t");
                toStr.append(entry.getValue()).append("\t");
                toStr.append(entry.getKey().getCraftName()).append("\n");
            }

            toStr.append("\nTotal turn-ins: ").append(calcTotalCrafts()).append(" Unused mats: ").append(getWastedMatsFromSolution(this, totalMats) * MAT_MULTIPLIER)
            .append(" Gems needed: ").append(getGemsFromSolution(this));

            return toStr.toString();
        } 
    }
}