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

        Solution bestSolution = getBestBruteForce(totalMats);

        if(bestSolution == null)
        {
            System.out.println("No crafts can be made with those materials. :(");
        }
        else
        {
            System.out.println(bestSolution.getSummary(totalMats));
        }
        

        System.out.println("Press enter to exit...");

        sc.nextLine();
        sc.close();
    }

    public static Solution getBestBruteForce(Solution current, Crafter crafter, Map<Material, Integer> mats, Set<Solution> calculatedSolutions)
    {
        if(!current.canFitCrafter(crafter, mats))
        {
            return null;
        }
        
        current.addCraft(crafter);
        if(!calculatedSolutions.add(current))
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
            return getBestSolution(mats, getBestBruteForce(new Solution(current), CRP, mats, calculatedSolutions),
            getBestBruteForce(new Solution(current), BSM, mats, calculatedSolutions),
            getBestBruteForce(new Solution(current), ARM, mats, calculatedSolutions),
            getBestBruteForce(new Solution(current), GSM, mats, calculatedSolutions),
            getBestBruteForce(new Solution(current), LTW, mats, calculatedSolutions),
            getBestBruteForce(new Solution(current), WVR, mats, calculatedSolutions),
            getBestBruteForce(new Solution(current), ALC, mats, calculatedSolutions),
            getBestBruteForce(new Solution(current), CUL, mats, calculatedSolutions)); 
        }

        return current;
    }

    public static Solution getBestBruteForce(Map<Material,Integer> mats)
    {
        Set<Solution> calculatedSolutions = new HashSet<>();

        long firstTimestamp = System.currentTimeMillis();
        Solution crp = getBestBruteForce(new Solution(), CRP, mats, calculatedSolutions);
        Solution bsm = getBestBruteForce(new Solution(), BSM, mats, calculatedSolutions);
        Solution arm = getBestBruteForce(new Solution(), ARM, mats, calculatedSolutions);
        Solution gsm = getBestBruteForce(new Solution(), GSM, mats, calculatedSolutions);
        Solution ltw = getBestBruteForce(new Solution(), LTW, mats, calculatedSolutions);
        Solution wvr = getBestBruteForce(new Solution(), WVR, mats, calculatedSolutions);
        Solution alc = getBestBruteForce(new Solution(), ALC, mats, calculatedSolutions);
        Solution cul = getBestBruteForce(new Solution(), CUL, mats, calculatedSolutions);

        Solution bestSolution = getBestSolution(mats, crp, bsm, arm, gsm, ltw, wvr, alc, cul); 
        System.out.println("Took "+(System.currentTimeMillis() - firstTimestamp)+"ms and calculated results of "+calculatedSolutions.size()+" combinations");
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

            int weightedValue = getWeightedBadness(solution, mats);

            if(bestSolution == null || weightedValue < bestWeightedValue)
            {
                bestSolution = solution;
                bestWeightedValue = weightedValue;
            }
        }

        return bestSolution;
    }

    public static int getWeightedBadness(Solution solution, Map<Material,Integer> mats)
    {
        int currentGems = getGemsFromSolution(solution);
        int currentWastedMats = getWastedMatsFromSolution(solution, mats);

        return currentGems * GEM_WEIGHT / 4 + currentWastedMats * WASTED_MAT_WEIGHT;
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

        public int calcTotalCrafts()
        {
            int numCrafts = 0;
            for(Map.Entry<Crafter,Integer> entry : crafts.entrySet())
            {
                numCrafts += entry.getValue();
            }
            return numCrafts;
        }

        public int hashCode()
        {
            return crafts.hashCode();
        }

        public boolean equals(Object o)
        {
            if(!(o instanceof Solution))
                return false;

            return ((Solution)o).crafts.equals(crafts);
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