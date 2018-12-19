package quaternary.youreanexpertharry.heck;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import quaternary.youreanexpertharry.YoureAnExpertHarry;
import quaternary.youreanexpertharry.settings.YAEHSettings;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Heck {
	public static Random random = new Random();
	static List<Item> allItems;
	
	public static void doHeck() throws Heckception {
		YAEHSettings settings = YoureAnExpertHarry.settings;
		HeckData allHeck = new HeckData(settings);
		
		Collection<Item> allItemsCollection = ForgeRegistries.ITEMS.getValuesCollection();
		allItems = new ArrayList<>(allItemsCollection);
		List<HeckTier> tiers = new ArrayList<>();
		for (int i = 0; i <= settings.topDifficulty; i++) {
			tiers.add(new HeckTier(i));
		}
		//Set<GoodItemStack> toAddRecipesFor = new HashSet<>();
		//Set<GoodItemStack> toAddRecipesForNext = new HashSet<>();
		//Set<GoodItemStack> bannedItems = new HashSet<>();
		//Set<GoodItemStack> allGoalItems = new HashSet<>();
		//Set<GoodItemStack> allBaseItems = new HashSet<>();
		//Set<AbstractHeckMethod> usedMethods = new HashSet<>();

		//If 0, put in bannedItems; if greater, put in specific categories.
		//If it's banned at tier 1 then it'll never be chosen. If it's banned at tier 2 then it'll be
		//for (HeckTier.TierItemStack tis : settings.bannedItems) {
		//if (tis.tier == 0) {
		//bannedItems.add(new GoodItemStack(tis));
		//}
		//else for (int i = tis.tier; i <= settings.topDifficulty; i++) {
		//tiers.get(i).bannedItems.add(new GoodItemStack(tis));
		//}
		//}

		//Test in mind to check if a goalItem for a tier should be banned at that tier.
		//Wait! Shouldn't it be chooseable at any tier as long as it's been made in a previous tier?
		//So we shouldn't ban it! But we have to make sure it doesn't get added in any tier higher than it should be.
		//for (HeckTier.TierItemStack tis : settings.goalItems) {
		//GoodItemStack gis = new GoodItemStack(tis);
		//allGoalItems.add(gis);
		//if (tis.tier == 0 || tis.tier == settings.topDifficulty) {
		//toAddRecipesFor.add(gis);
		//}
		//else {
		//tiers.get(tis.tier).goalItems.add(gis);
				//for (int i = tis.tier + 1; i <= settings.topDifficulty; i++) {
				//	tiers.get(i).bannedItems.add(new GoodItemStack(tis));
				//}
		//}
		//}

		//for (HeckTier.TierItemStack tis : settings.allBaseItems) {
		//GoodItemStack gis = new GoodItemStack(tis);
		//allBaseItems.add(gis);
		//}
		
		//don't use a top tier item in another top tier item recipe
		//for that VARIED GAMEPLAY
		//bannedItems.addAll(toAddRecipesFor);
		
		List<String> zenBody = new ArrayList<>();
		int recipeCount = 0;

		//We need to ensure that all the base items from tier 1 have recipes. That will be... tough.
		//We need base items that the mod can use.
		while(allHeck.currentLevel >= 1) {
			zenBody.add("// RECIPE LEVEL: " + allHeck.currentLevel + "\n\n");

			//don't use these items within this tier or in future recipes
			allHeck.toAddRecipesFor.forEach(outputGood -> allHeck.bannedItems.add(outputGood));


			for(GoodItemStack outputGood : allHeck.toAddRecipesFor) {
				ItemStack output = outputGood.actualStack;

				List<ItemStack> recipeStacks = new ArrayList<>();
				AbstractHeckMethod method = chooseMethod(settings, allHeck.currentLevel, null);
				allHeck.usedMethods.add(method);

				for (int i = 0; i < 6; i++) {
					Pair<List<ItemStack>, Boolean> attempt;
					attempt = method.chooseInputs(allHeck, outputGood, false);
					if (attempt.getRight() == true) {
						recipeStacks = attempt.getLeft();
						allHeck.usedMethods.add(method);
						break;
					}
					method = chooseMethod(settings, allHeck.currentLevel, method);
				}

				if (recipeStacks.size() == 0) throw new Heckception("ran out of possible recipes, somehow!");






				//else {
					//recipeStacks = new ArrayList<>(method.inputCount);
					//for (int a = 0; a < method.inputCount; a++) {
						//recipeStacks.add(chooseItem(allHeck.bannedItems, allHeck.tiers.get(allHeck.currentLevel).bannedItems, allHeck.allBaseItems, outputGood, false));
					//}
				//}
				
				StringBuilder b = new StringBuilder();
				
				b.append("//Recipe ");
				b.append(recipeCount);
				b.append('\n');
				
				b.append(method.removeRecipe(output));
				b.append('\n');
				
				b.append(method.writeZenscript("youre_an_expert_harry_" + recipeCount, output, recipeStacks));
				b.append('\n');
				
				zenBody.add(b.toString());
				
				recipeCount++;
				
				//mark all of the items added in this recipe as candidates for items to add next turn
				//Say we're in tier 5 and obsidian is a tier 2 goal item and it gets added to the recipe.
				//It's contained in allGoalItems, so it doesn't get added to toAddRecipesForNext.
				//But when we get to tier 2 then it will get added.
				//If we're in tier 1 obsidian will already be banned because it was added as a recipe in tier 2.
				for (ItemStack is : recipeStacks) {
					GoodItemStack gis = new GoodItemStack(is);
					if (!(allHeck.allGoalItems.contains(gis)) && !(allHeck.allBaseItems.contains(gis))) {
						allHeck.toAddRecipesForNext.add(gis);
					}
				}
			}
			
			allHeck.currentLevel--;
			allHeck.toAddRecipesFor.clear();
			allHeck.toAddRecipesFor.addAll(allHeck.toAddRecipesForNext);
			//Adds all the relevant goal items to the next tier.
			allHeck.toAddRecipesFor.addAll(allHeck.tiers.get(allHeck.currentLevel).goalItems);
			allHeck.toAddRecipesForNext.clear();
		}

		zenBody.add("// RECIPE LEVEL: Base" + "\n\n");

		//don't use these items within this tier or in future recipes
		allHeck.toAddRecipesFor.forEach(outputGood -> allHeck.bannedItems.add(outputGood));

		for(GoodItemStack outputGood : allHeck.toAddRecipesFor) {
			ItemStack output = outputGood.actualStack;

			AbstractHeckMethod method = chooseMethod(settings, 1, null);
			allHeck.usedMethods.add(method);

			List<ItemStack> recipeStacks = new ArrayList<>();

			for (int i = 0; i < 6; i++) {
				Pair<List<ItemStack>, Boolean> attempt;
				attempt = method.chooseInputs(allHeck, outputGood, true);
				if (attempt.getRight() == true) {
					recipeStacks = attempt.getLeft();
					allHeck.usedMethods.add(method);
					break;
				}
				method = chooseMethod(settings, 1, method);
			}

			if (recipeStacks.size() == 0) throw new Heckception("ran out of possible recipes, somehow!");

			StringBuilder b = new StringBuilder();

			b.append("//Recipe ");
			b.append(recipeCount);
			b.append('\n');

			b.append(method.removeRecipe(output));
			b.append('\n');

			b.append(method.writeZenscript("youre_an_expert_harry_" + recipeCount, output, recipeStacks));
			b.append('\n');

			zenBody.add(b.toString());

			recipeCount++;
		}


		
		StringBuilder header = new StringBuilder();
		allHeck.usedMethods.forEach(a -> a.getRequiredImports().ifPresent(i -> {
			header.append(i);
			header.append('\n');
		}));
		
		//I'm really bad a java files pls halp.
		File mainFolder = YoureAnExpertHarry.settingsFile.getParentFile().getParentFile();
		File scriptsFolder = new File(mainFolder.getAbsolutePath() + File.separator + "scripts");
		scriptsFolder.mkdirs();
		splitAndWriteZenscript(header.toString(), zenBody, scriptsFolder);
		
		YoureAnExpertHarry.LOGGER.info("Done");
	}

	//Add disallowed recipes
	private static AbstractHeckMethod chooseMethod(YAEHSettings settings, int currentLevel, AbstractHeckMethod disallowedMethod) throws Heckception {
		List<AbstractHeckMethod> methods;

		if (currentLevel > 0) {
			methods = settings.heckMethods.stream()
					.filter(p -> currentLevel <= p.maxLevel && currentLevel >= p.minLevel)
					.map(p -> p.method)
					.collect(Collectors.toList());
		} else {
			methods = new ArrayList<>();
			methods.add(HeckMethods.FOUR_WAY_SYMMETRICAL_THREE_BY_THREE);
			methods.add(HeckMethods.SHAPED_THREE_BY_THREE);
			methods.add(HeckMethods.SHAPELESS_TWO_BY_TWO);
		}

		methods.remove(disallowedMethod);
		
		if(methods.size() == 0) throw new Heckception("No heckmethods available for level " + currentLevel);
		else return methods.get(random.nextInt(methods.size()));
	}


	public static ItemStack chooseItem(HeckData allHeck, GoodItemStack alsoBannedItem, boolean base) throws Heckception {
		if (base) return chooseBaseItem(allHeck.allBaseItems);
		Set<GoodItemStack> tierBaseItems = allHeck.tiers.get(allHeck.currentLevel).baseItems;
		for(int tries = 0; tries < 1000; tries++) {
			GoodItemStack bep;
			ItemStack hahayes;

			//Tries to add a baseItem from the tier with a chance that scales with number of items in tier.
			//Next up, change baseList so it actually has all the base items from every tier lower than the current one.
			if (random.nextInt((YoureAnExpertHarry.settings.topDifficulty - allHeck.currentLevel + 1) * 10) == 1
					&& tierBaseItems.size() > 0) {
				ArrayList<GoodItemStack> baseList = new ArrayList<>();
				baseList.addAll(tierBaseItems);
				bep = baseList.get(random.nextInt(baseList.size()));
				hahayes = bep.actualStack;
			} else {
				Item i = allItems.get(random.nextInt(allItems.size()));
				int data;
				if(i.getHasSubtypes()) {
					NonNullList<ItemStack> choices = NonNullList.create();
					i.getSubItems(i.getCreativeTab(), choices);
					if(choices.isEmpty()) data = 0;
					else data = choices.get(random.nextInt(choices.size())).getMetadata();
				} else {
					data = 0;
				}

				hahayes = new ItemStack(i, 1, data);
				bep = new GoodItemStack(hahayes);
			}

			if(!hahayes.isEmpty() && !allHeck.bannedItems.contains(bep) && !allHeck.tiers.get(allHeck.currentLevel).bannedItems.contains(bep) && !alsoBannedItem.equals(bep)) return hahayes;
		}
		
		throw new Heckception("Ran out of input items for recipes (couldn't find a fresh item to add to a recipe after 1000 tries). Either your difficulty is set too high, or you are just unlucky");
	}


	private static ItemStack chooseBaseItem(Set<GoodItemStack> baseItems) {
		ArrayList<GoodItemStack> base = new ArrayList<>();
		base.addAll(baseItems);
		Item i = base.get(random.nextInt(base.size())).actualStack.getItem();
		int data;
		if (i.getHasSubtypes()) {
			NonNullList<ItemStack> choices = NonNullList.create();
			i.getSubItems(i.getCreativeTab(), choices);
			if(choices.isEmpty()) data = 0;
			else data = choices.get(random.nextInt(choices.size())).getMetadata();
		} else {
			data = 0;
		}

		return new ItemStack(i, 1, data);
	}
	
	public static class GoodItemStack {
		public GoodItemStack(ItemStack actualStack) {
			this.actualStack = actualStack;
		}
		public GoodItemStack(HeckTier.TierItemStack actualStack) {this.actualStack = actualStack.stack;}
		
		public ItemStack actualStack;
		
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof GoodItemStack) {
				ItemStack other = ((GoodItemStack)obj).actualStack;
				return other.getItem() == actualStack.getItem() && other.getMetadata() == actualStack.getMetadata();
			} else return false;
		}
		
		@Override
		public int hashCode() {
			return actualStack.getItem().getRegistryName().hashCode() + actualStack.getMetadata() * 1232323;
		}
	}
	
	private static final int LINES_PER_FILE = 150;
	
	public static void splitAndWriteZenscript(String header, List<String> lines, File scriptsFolder) throws Heckception {
		int fileCount = MathHelper.ceil(lines.size() / (float) LINES_PER_FILE);
		
		for(int i = 0; i < fileCount; i++) {
			StringBuffer b = new StringBuffer();
			b.append("#priority ");
			b.append(fileCount + 5 - i);
			b.append('\n');
			b.append(header);
			
			int from = Math.min(i * LINES_PER_FILE, lines.size());
			int to = Math.min((i + 1) * LINES_PER_FILE, lines.size());
			lines.subList(from, to).forEach(s -> {
				b.append(s);
				b.append('\n');
			});
			
			try {
				File outputFile = new File(scriptsFolder.getAbsolutePath() + File.separator + "youre_an_expert_harry_" + i + ".zs");
				if(outputFile.exists()) {
					YoureAnExpertHarry.LOGGER.info("Deleting " + outputFile.getAbsolutePath());
					outputFile.delete();
				}
				
				YoureAnExpertHarry.LOGGER.info("Creating " + outputFile.getAbsolutePath());
				outputFile.createNewFile();
				YoureAnExpertHarry.LOGGER.info("Writing");
				
				try(FileWriter writer = new FileWriter(outputFile)) {
					writer.write(b.toString());
				}
			} catch(Exception eee) {
				YoureAnExpertHarry.LOGGER.error(eee);
				throw new Heckception("Couldn't write output file number " + i + " hmm (check log)");
			}
		}
	}
}
