package ishgard;

import static ishgard.Crafter.*;
import static ishgard.Material.*;

import java.util.Map;
import java.util.Scanner;
import java.util.HashMap;

import java.util.Set; 
import java.util.HashSet; 


public class IshgardSolver
{
    public static final int WASTED_MAT_WEIGHT = 2;
    public static final int GEM_WEIGHT = 1;

    public static Set<Integer> calculatedBranches = new HashSet<>();

    public static Map<Crafter, Recipe> RECIPES = new HashMap<>();
            

    public static Map<Material, Integer> MATS = new HashMap<>();
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
        Scanner sc = new Scanner(System.in);
        for(Material m : Material.values())
        {
            System.out.println("How many "+m+"s do you have?");
            MATS.put(m, sc.nextInt() / 10);
        }
        sc.nextLine();
        
        int accumulator = 1;

        long firstTimestamp = System.currentTimeMillis();
        long timestamp = firstTimestamp;
        Solution crp = getBestSolution(new Solution(), CRP, accumulator);
        System.out.println("Took "+(System.currentTimeMillis() - timestamp)+"ms for CRP");
        timestamp = System.currentTimeMillis();
        Solution bsm = getBestSolution(new Solution(), BSM, accumulator);
        System.out.println("Took "+(System.currentTimeMillis() - timestamp)+"ms for BSM");
        timestamp = System.currentTimeMillis();
        Solution arm = getBestSolution(new Solution(), ARM, accumulator);
        System.out.println("Took "+(System.currentTimeMillis() - timestamp)+"ms for ARM");
        timestamp = System.currentTimeMillis();
        Solution gsm = getBestSolution(new Solution(), GSM, accumulator);
        System.out.println("Took "+(System.currentTimeMillis() - timestamp)+"ms for GSM");
        timestamp = System.currentTimeMillis();
        Solution ltw = getBestSolution(new Solution(), LTW, accumulator);
        System.out.println("Took "+(System.currentTimeMillis() - timestamp)+"ms for LTW");
        timestamp = System.currentTimeMillis();
        Solution wvr = getBestSolution(new Solution(), WVR, accumulator);
        System.out.println("Took "+(System.currentTimeMillis() - timestamp)+"ms for WVR");
        timestamp = System.currentTimeMillis();
        Solution alc = getBestSolution(new Solution(), ALC, accumulator);
        System.out.println("Took "+(System.currentTimeMillis() - timestamp)+"ms for ALC");
        timestamp = System.currentTimeMillis();
        Solution cul = getBestSolution(new Solution(), CUL, accumulator);
        System.out.println("Took "+(System.currentTimeMillis() - timestamp)+"ms for CUL");

        Solution bestSolution = getBestSolution(crp, bsm, arm, gsm, ltw, wvr, alc, cul); 

        System.out.println("Took "+(System.currentTimeMillis() - firstTimestamp)+"ms and calculated results of "+calculatedBranches.size()+" combinations");

        System.out.println(bestSolution);
        System.out.println("Press enter to exit...");

        sc.nextLine();
        sc.close();
    }


    public static Solution getBestSolution(Solution current, Crafter crafter, int accumulator)
    {
        if(!current.canFitCrafter(crafter))
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
            if(current.canFitCrafter(craft))
            {
                canContinue = true;
                break;
            }
        }

        if(canContinue)
        {
            return getBestSolution(getBestSolution(new Solution(current), CRP, accumulator),
            getBestSolution(new Solution(current), BSM, accumulator),
            getBestSolution(new Solution(current), ARM, accumulator),
            getBestSolution(new Solution(current), GSM, accumulator),
            getBestSolution(new Solution(current), LTW, accumulator),
            getBestSolution(new Solution(current), WVR, accumulator),
            getBestSolution(new Solution(current), ALC, accumulator),
            getBestSolution(new Solution(current), CUL, accumulator)); 
        }

        return current;
    }

    public static int getGemsFromSolution(Solution s)
    {
        int gems = 0;
        for(Map.Entry<Crafter,Integer> entry : s.getSolution().entrySet())
        {
            gems += RECIPES.get(entry.getKey()).numGems * (entry.getValue() == null ? 0 : entry.getValue());
        }
        return gems;
    }

    public static int getWastedMatsFromSolution(Solution s)
    {
        int mats = 0;
        for(Material mat : Material.values())
        {
            int numMatsInvolved = s.getMats().get(mat) == null? 0 : s.getMats().get(mat);
            
            mats += MATS.get(mat) - numMatsInvolved;
        }
        return mats;
    }

    public static Solution getBestSolution(Solution... solutions)
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
            int currentWastedMats = getWastedMatsFromSolution(solution);

            int weightedValue = currentGems * GEM_WEIGHT / 4 + currentWastedMats * WASTED_MAT_WEIGHT;
            //System.out.println("solution has "+currentWastedMats+" wasted mats and "+currentGems+" gems. This gives it a weighted badness value of "+weightedValue);

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
        Map<Crafter,Integer> solution;
        Map<Material,Integer> matsUsed;

        public Solution(Solution toClone)
        {
            solution = new HashMap<>(toClone.getSolution());
            matsUsed = new HashMap<>(toClone.getMats());
        }
        public Solution()
        {
            solution = new HashMap<>();
            matsUsed = new HashMap<>();
        }

        public boolean canFitCrafter(Crafter craft)
        {
            for(Material m : RECIPES.get(craft).mats)
            {
                if((matsUsed.get(m) == null ? 0 : matsUsed.get(m)) + 1 > MATS.get(m))
                {
                    return false;
                }
            }

            return true;
        }

        public void addCraft(Crafter craft)
        {
            addValueToMap(solution, craft, 1);
            for(Material m : RECIPES.get(craft).mats)
            {
                addValueToMap(matsUsed, m, 1);
            }
        }

        public Map<Crafter,Integer> getSolution()
        {
            return solution;
        }

        public Map<Material,Integer> getMats()
        {
            return matsUsed;
        }

        public int calcTotalCrafts()
        {
            int numCrafts = 0;
            for(Map.Entry<Crafter,Integer> entry : solution.entrySet())
            {
                numCrafts += entry.getValue();
            }
            return numCrafts;
        }

        public String toString()
        {
            StringBuilder toStr = new StringBuilder();
            for(Map.Entry<Crafter,Integer> entry : solution.entrySet())
            {
                toStr.append(entry.getKey()).append("\t");
                toStr.append(entry.getValue()).append("\t");
                toStr.append(entry.getKey().getCraftName()).append("\n");
            }

            toStr.append("\nTotal turn-ins: ").append(calcTotalCrafts()).append(" Unused Mats: ")
            .append(getWastedMatsFromSolution(this) * 10).append(" Gems needed: ").append(getGemsFromSolution(this));

            return toStr.toString();
        }
        
    }
}
