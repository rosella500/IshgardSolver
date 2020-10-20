package ishgard;

import static ishgard.Crafter.*;
import static ishgard.Material.*;

import java.util.Map;
import java.util.Scanner;
import java.util.EnumMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
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
        
        Solution bestSolution = getBestGreedy(totalMats);

        System.out.println();
        if(bestSolution == null)
        {
            System.out.println("No crafts can be made with those materials. :(");
        }
        else
        {
            System.out.println(bestSolution.getSummary(totalMats));
            //Map<Material,Integer> leftoverMats = bestSolution.getLeftoverMats(totalMats);
            //System.out.println("Leftover mats: "+leftoverMats);
        }

        System.out.println("Press enter to exit...");

        sc.nextLine();
        sc.close();
    }

    public static <E> List<List<E>> generatePerm(List<E> original) {
        if (original.isEmpty()) {
          List<List<E>> result = new ArrayList<>(); 
          result.add(new ArrayList<>()); 
          return result; 
        }
        E firstElement = original.remove(0);
        List<List<E>> returnValue = new ArrayList<>();
        List<List<E>> permutations = generatePerm(original);
        for (List<E> smallerPermutated : permutations) {
          for (int index=0; index <= smallerPermutated.size(); index++) {
            List<E> temp = new ArrayList<>(smallerPermutated);
            temp.add(index, firstElement);
            returnValue.add(temp);
          }
        }
        return returnValue;
      }

    public static Solution getBestGreedy(Map<Material, Integer> totalMats)
    {
        List<Solution> allSolutions = new ArrayList<>();
        
        List<Crafter> crafters = new ArrayList<>();
        for(Crafter c : Crafter.values())
        {
            crafters.add(c);
        }
        List<List<Crafter>> listOfAllPermuts = generatePerm(crafters);
        
        for(List<Crafter> crafterList : listOfAllPermuts)
        {
            Solution greedySolution = new Solution();
            for(Crafter craft : crafterList)
            {
                while(greedySolution.canFitCrafter(craft, totalMats))
                {
                    greedySolution.addCraft(craft);
                }
            }
            if(greedySolution.getCrafts().size() > 0)
            {
                allSolutions.add(greedySolution);
            }
            
        }

        return getBestSolution(totalMats, allSolutions.toArray(new Solution[0]));

    }

    public static Solution getBestGreedyWithOptimizations(Map<Material, Integer> totalMats, int degrees)
    {

        Solution greedySolution = getBestGreedy(totalMats);

        
        if(greedySolution == null)
        {
            return null;
        }

        //int degrees = Collections.max(greedySolution.crafts.values());

        Set<Solution> calculatedSolutions = new HashSet<>();
        calculatedSolutions.add(greedySolution);

        long firstTimestamp = System.currentTimeMillis();
        Solution bestAdjacent = getBestAdjacentSolution(greedySolution, degrees, totalMats, calculatedSolutions);

        Solution bestSolution = greedySolution;
        if(bestAdjacent != null && getWeightedBadness(bestAdjacent, totalMats) < getWeightedBadness(greedySolution, totalMats))
        {
            bestSolution = bestAdjacent;
            System.out.println("Found optimization over greedy! Original solution: "+greedySolution.getSummary(totalMats));
        }

        System.out.println("Took "+(System.currentTimeMillis() - firstTimestamp)+"ms and calculated results of "+calculatedSolutions.size()+" combinations. Degrees from greedy: "+degrees);
        return bestSolution;
    }

    public static Solution getBestAdjacentSolution(Solution partialSolution, int degree, Map<Material, Integer> mats, Set<Solution> calculatedSolutions)

    {
        if(partialSolution == null)
        {
            return null;
        }

        if(degree == 0)
        {
            return partialSolution;
        }

        List<Solution> possibleSolutions = new ArrayList<>();
        possibleSolutions.add(partialSolution);

        //Find all adjacent points
        for(Crafter craftToRemove: Crafter.values())
        {
            Solution solutionRemoved = partialSolution.cloneMinusCraft(craftToRemove);
            if(solutionRemoved == null)
            {
                continue;
            }

            Solution bestSolution;
            if(degree == 1)
            {
                if(calculatedSolutions.contains(solutionRemoved))
                { 
                    //Already calculated
                    continue;
                }
                for(Crafter craftToAdd : Crafter.values())
                {
                    bestSolution = getBestBruteForce(solutionRemoved, craftToAdd, mats, calculatedSolutions);   

                    if(bestSolution != null)
                    {
                        possibleSolutions.add(bestSolution);
                    }  
                }
            }
            else
            {
                bestSolution = getBestAdjacentSolution(solutionRemoved, degree - 1, mats, calculatedSolutions);
                if(bestSolution != null)
                {
                    possibleSolutions.add(bestSolution);
                }  
            }  

            
        }
        
        return getBestSolution(mats, possibleSolutions.toArray(new Solution[0]));
    }

    public static Solution getBestBruteForceWithDivisions(Map<Material, Integer> totalMats, int groupSize)
    {
        int subdivision = groupSize / MAT_MULTIPLIER;
        int mult = -1;
        for(Map.Entry<Material, Integer> entry : totalMats.entrySet())
        {
            int tempMult = entry.getValue() / subdivision;

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
        

        Solution bestSolution = getBestBruteForce(workingMatMap);
        bestSolution.mult(mult);

        Map<Material,Integer> leftoverMats = bestSolution.getLeftoverMats(totalMats);
        System.out.println("Leftover mats: "+leftoverMats);
        Solution leftoverSolution = getBestBruteForce(leftoverMats);

        bestSolution.add(leftoverSolution);

        return bestSolution;
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

        public Solution cloneMinusCraft(Crafter craft)
        {
            Solution clone = new Solution(this);
            if(clone.crafts.containsKey(craft) && clone.crafts.get(craft) > 0)
            {
                clone.crafts.put(craft, clone.crafts.get(craft) - 1);
                for(Material mat : RECIPES.get(craft).mats)
                {
                    clone.matsUsed.put(mat, clone.matsUsed.get(mat) - 1);
                }

                return clone;
            }
            else
            {
                return null;
            }
        }

        public int hashCode()
        {
            return crafts.hashCode();
        }

        public boolean equals(Object o)
        {
            if(o==this)
                return true;
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