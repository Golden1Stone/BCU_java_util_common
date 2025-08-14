package common.battle.data;

import common.CommonStatic;
import common.io.json.JsonClass;
import common.io.json.JsonDecoder.OnInjected;
import common.io.json.JsonField;
import common.pack.Context.ErrType;
import common.pack.Identifier;
import common.pack.UserProfile;
import common.system.files.VFile;
import common.util.Data;
import common.util.Data.Proc.ProcItem;
import common.util.unit.Trait;
import common.util.unit.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;

@JsonClass(read = JsonClass.RType.FILL)
public class PCoin extends Data {
	public static int TALENT_ID_MIN = 1;
	public static int TALENT_ID_MAX;
	public static int MODIFIER_MAX = 5;
	public static int TALENT_COUNT_MAX = 8;
	public static void read() {
		Queue<String> qs = VFile.readLine("./org/data/SkillAcquisition.csv");

		qs.poll();

		for (String str : qs) {
			String[] strs = str.trim().split(",");

			if (strs.length >= 2) {
				int[] data = CommonStatic.parseIntsN(str);
				Unit u = Identifier.parseInt(data[0], Unit.class).get();

				if (u != null) {
					for (int i = 2; i < u.forms.length; i++)
						new PCoin(data, u.forms[i].du);
				}
				TALENT_ID_MAX = Math.max(TALENT_ID_MAX, data[2]);
			}
		}
	}

	private final MaskUnit du;
	public MaskUnit full = null;
	@JsonField(generic = Trait.class, alias = Identifier.class, io = JsonField.IOType.R)
	@Deprecated
	public ArrayList<Trait> trait = new ArrayList<>();
	@JsonField(generic = int[].class, io = JsonField.IOType.R)
	@Deprecated
	public ArrayList<int[]> info;

	@JsonField(generic = int[].class)
	public final ArrayList<int[]> data = new ArrayList<>(); // List{ { id, name, modif0... } } -> { { abilityId, maxLvl, isUltra } }
	@JsonField(alias = Identifier.class)
	public Trait[][] traits; // { {  }, { Trait.RED, ... }, ... }
	@JsonField
	public int[][][] modifiers;

	public PCoin(CustomEntity ce) {
		du = (CustomUnit)ce;
		((CustomUnit)du).pcoin = this;
	}

	// TODO: commenting this out until i figure out if it's safe to delete
//	public PCoin(String[] strs, MaskUnit du) { // leaving reminder that this is unused at least in PC ver.
//		trait = Trait.convertType(CommonStatic.parseIntN(strs[1]));
//		for (int i = 0; i < 8; i++) {
//			if(talentExist(strs, 2 + i * 14)) {
//				info.add(new int[14]);
//
//				for (int j = 0; j < 14; j++) {
//					int v = CommonStatic.parseIntN(strs[2 + i * 14 + j]);
//					info.get(info.size() - 1)[j] = v;
//				}
//			}
//		}
//
//		max = info.stream().mapToInt(i -> Math.max(1, i[1])).toArray();
//		this.du = du;
//
//		full = improve(max);
//	}

	private PCoin(int[] strs, MaskUnit du) {
		trait = Trait.talentBitmaskToTrait(strs[1]);

		for (int i = 0; i < TALENT_COUNT_MAX; i++) {
			if (2 + i * 14 >= strs.length) {
				break;

			if(strs[2 + i * 14] != 0) {
				int startIndex = 2 + i * 14;
				int[] infoData = new int[3];

				infoData[0] = strs[startIndex]; // abilityId
				infoData[1] = Math.max(1, strs[startIndex + 1]); // max lvl
				infoData[2] = strs[13 + startIndex]; //
				data.add(infoData);
			}
		}
		// all of data should be iterated now
		modifiers = new int[getTalentCount()][MODIFIER_MAX][2];
		traits = new Trait[getTalentCount()][0];
		for (int i = 0; i < getTalentCount(); i++) {
			int startIndex = 2 + i * 14;
			for (int j = 0; j < modifiers[i].length; j++) {
				modifiers[i][j][0] = strs[startIndex + 2 + j * 2];
				modifiers[i][j][1] = strs[startIndex + 3 + j * 2];
			}
			int talentId = data.get(i)[0];
			if (talentId > 0 && talentId < PC_CORRES.length && PC_CORRES[talentId][1] == P_MINIWAVE)
				if (modifiers[i][3][0] == 0 && modifiers[i][3][1] == 0) {
					modifiers[i][3][0] = 20;
					modifiers[i][3][1] = 20;
				}
			if (strs[startIndex + 12] > 0)
				traits[i] = Trait.convertTalentType(strs[1]).toArray(new Trait[0]);
		}

		this.du = du;
		((DataUnit) du).pcoin = this;
		updateMax();
	}

	public int[] getMaxLvls() {
		return data.stream().mapToInt(i -> i[1]).toArray();
	}

	public void updateMax() {
		full = improve(getMaxLvls());
	}

	public void verify() {
		for (int[] data : data) { // { id, maxLv, ultra }
			Proc proc = du.getAllProc();
			data[1] = Math.max(data[1], 1);
//			int[] talentRes = Data.PC_CORRES[data[0]];
//			int type = talentRes[1];
//
//			if (Data.PC_CORRES[data[0]][3] != -1) {
//				data[1] = Data.PC_CORRES[Data.PC_CORRES[data[0]][3]][1];
//				data[2] = data[3] = 100 - proc.getArr(type).get(0);
//				return;
//			}
//
//			if (talentRes[0] == PC_P) {
//				switch (talentRes[1]) {
//
//				}
//			}
//
//
//			switch (data[0]) { // todo: use editorgroup (please)
//				case 0:
//					break;
//				case 56: case 65: // normalize surge chance
//					data[2] = MathUtil.clip(data[2], 0, 100 - proc.getArr(type).get(0));
//					data[3] = MathUtil.clip(data[3], data[2], 100 - proc.getArr(type).get(0));
//					data[8] = Math.max(1, data[8] / Data.VOLC_ITV) * Data.VOLC_ITV;
//					data[9] = Math.max(Math.max(1, data[9] / Data.VOLC_ITV) * Data.VOLC_ITV, data[8]);
//					break;
//				case 10:
//					data[2] = MathUtil.clip(data[2], 0, 100 - proc.getArr(type).get(0));
//					data[3] = MathUtil.clip(data[3], data[2], 100 - proc.getArr(type).get(0));
//					data[4] = Math.max(data[4], 0);
//					data[5] = Math.max(data[5], data[4]);
//					break;
//				case 61:
//					data[2] = MathUtil.clip(data[2], 0, 100);
//					data[3] = MathUtil.clip(data[3], data[2], 100);
//					break;
//				case 25: case 26: case 31: case 32:
//					data[2] = Math.max(data[2], 0);
//					data[3] = Math.max(data[3], data[2]);
//					break;
//				case 64:
//					data[2] = MathUtil.clip(data[2], 0, 100 - proc.getArr(type).get(1));
//					data[3] = MathUtil.clip(data[3], data[2], 100 - proc.getArr(type).get(1));
//					data[4] = Math.max(data[4], 0);
//					data[5] = Math.max(data[5], data[4]);
//					break;
//				case 62: case 1:
//					data[6] = Math.max(data[6], 0);
//					data[7] = Math.max(data[7], data[6]);
//				case 2: case 3: case 9: case 17: case 50: case 51: case 60:
//					data[4] = Math.max(data[4], 0);
//					data[5] = Math.max(data[5], data[4]);
//				case 8: case 11: case 13: case 15: case 18: case 19: case 20: case 21: case 22: case 24: case 30:
//				case 52: case 54: case 58:
//					data[2] = MathUtil.clip(data[2], 0, 100 - proc.getArr(type).get(0));
//					data[3] = MathUtil.clip(data[3], data[2], 100 - proc.getArr(type).get(0));
//					break;
//			}
		}
	}

	@SuppressWarnings("deprecation")
	public MaskUnit improve(int[] talents) {
		MaskUnit ans = du.clone();

		int[] temp;
		int[] max = getMaxLvls();

		if (talents.length < max.length) {
			temp = new int[max.length];

			System.arraycopy(talents, 0, temp, 0, talents.length);
			System.arraycopy(max, talents.length, temp, talents.length, max.length - talents.length);
		} else {
			temp = talents.clone();
		}

		talents = temp;

		for (int i = 0; i < data.size(); i++) {
			int[] data = this.data.get(i);
			if (data[0] >= PC_CORRES.length) {
				CommonStatic.ctx.printErr(ErrType.NEW, "new PCoin ability not yet handled by BCU: " + this.data.get(i)[0] +"\nData is "+Arrays.toString(this.data.get(i)));
				continue; // todo: include all data including modifiers and traits
			}

			int[] type = PC_CORRES[data[0]];
			if (type[0] == -1) {
				CommonStatic.ctx.printErr(ErrType.NEW, "new PCoin ability not yet handled by BCU: " + this.data.get(i)[0] +"\nData is "+Arrays.toString(this.data.get(i)));
				continue;
			}

			if (talents[i] == 0) {
				if (type[0] == PC_TRAIT) {
					Trait types = UserProfile.getBCData().traits.get(type[1]);
					ans.getTraits().remove(types);
				}
				continue;
			} else {
				for (Trait t : traits[i])
					if (!ans.getTraits().contains(t))
						ans.getTraits().add(t);
			}

			int maxlv = data[1];
			int[] modifs = new int[modifiers[i].length];

			if (maxlv > 1) {
				for (int j = 0; j < modifs.length; j++) {
					int modif0 = modifiers[i][j][0];
					int modif1 = modifiers[i][j][1];
					modifs[j] = (modif1 - modif0) * (talents[i] - 1) / (maxlv - 1) + modif0;
				}
			} else
				for (int j = 0; j < modifs.length; j++)
					modifs[j] = modifiers[i][j][0];

			if (type[0] == PC_P) {
				ProcItem tar = ans.getProc().getArr(type[1]);

				if (type[1] == P_VOLC || type[1] == P_MINIVOLC) {
					if (du instanceof DataUnit) { // todo: restructure talents to account for more modifiers
						tar.set(0, modifs[0]);
						tar.set(1, modifs[2] / 4);
						tar.set(2, (modifs[2] + modifs[3]) / 4);
						tar.set(3, modifs[1] * 20);
						tar.set(4, modifs[1] * 20);

						if (type[1] == P_MINIVOLC) {
							tar.set(5, 20);
						}
					} else {
						tar.set(0, tar.get(0) + modifs[0]);
						tar.set(1, tar.get(1) + Math.min(modifs[1], modifs[2]));
						tar.set(2, tar.get(2) + Math.max(modifs[1], modifs[2]));
						tar.set(3, tar.get(3) + modifs[3]);

						if (type[1] == P_MINIVOLC) {
							tar.set(4, modifs[4]);
						}
					}
				} else if (type[1] == P_BSTHUNT) {
					tar.set(0, 1);
					tar.set(1, modifs[0]);
					tar.set(2, modifs[1]);
				} else if (type[1] == P_BLAST) {
					if (du instanceof DataUnit) {
						tar.set(0, modifs[0]);
						tar.set(1, modifs[1] / 4);
						tar.set(2, (modifs[1] + modifs[2]) / 4);
					} else {
						tar.set(0, tar.get(0) + modifs[0]);
						tar.set(1, tar.get(1) + Math.min(modifs[1], modifs[2]));
						tar.set(2, tar.get(2) + Math.max(modifs[1], modifs[2]));
					}
				} else if (type[1] == P_MINIWAVE && du instanceof DataUnit) {
					tar.set(0, tar.get(0) + modifs[0]);
					tar.set(1, tar.get(1) + modifs[1]);
					tar.set(3, tar.get(3) + modifs[2]);
				} else {
					for (int j = 0; j < 4; j++) {
						if (modifs[j] > 0) {
							tar.set(j, tar.get(j) + modifs[j]);
						}
					}
				}

				if (du instanceof DataUnit) {
					if (type[1] == P_STRONG && modifs[0] != 0)
						tar.set(0, 100 - tar.get(0));
					else if (type[1] == P_WEAK)
						tar.set(2, 100 - tar.get(2));
					else if (type[1] == P_BOUNTY)
						tar.set(0, 100);
					else if (type[1] == P_ATKBASE)
						tar.set(0, 300);
				} else if (!((CustomEntity)du).common && !(type[1] == P_STRONG && modifs[0] != 0)) {
					for (AtkDataModel atk : ((CustomEntity)ans).atks) {
						ProcItem atks = atk.proc.getArr(type[1]);

						if (type[1] == P_VOLC) {
							atks.set(0, modifs[0]);
							atks.set(1, Math.min(modifs[1], modifs[2]));
							atks.set(2, Math.max(modifs[1], modifs[2]));
							atks.set(3, modifs[3]);
						} else
							for (int j = 0; j < 4; j++)
								if (modifs[j] > 0)
									atks.set(j, atks.get(j) + modifs[j]);
					}
				}
			} else if (type[0] == PC_AB || type[0] == PC_BASE) {
				if (du instanceof DataUnit)
					improve((DataUnit)ans, type, modifs);
				else
					improve((CustomUnit)ans, type, modifs);
			} else if (type[0] == PC_IMU)
				ans.getProc().getArr(type[1]).set(0, 100);
			else if (type[0] == PC_TRAIT) {
				Trait types = UserProfile.getBCData().traits.get(type[1]);

				if (!ans.getTraits().contains(types))
					ans.getTraits().add(types);
			}
		}

		return ans;
	}

	private static void improve(DataUnit ans, int[] type, int[] modifs) {
		if (type[0] == PC_AB)
			ans.abi |= type[1];
		else {
			switch (type[1]) {
				case PC2_SPEED:
					ans.speed += modifs[0];
					break;
				case PC2_CD:
					ans.respawn -= modifs[0];
					break;
				case PC2_COST:
					ans.price -= modifs[0];
					break;
				case PC2_HB:
					ans.hb += modifs[0];
					break;
				case PC2_TBA:
					ans.tba = (int) (ans.tba * (100 - modifs[0]) / 100.0);
			}
		}
	}

	private static void improve(CustomUnit ans, int[] type, int[] modifs) {
		if (type[0] == PC_AB)
			ans.abi |= type[1];
		else {
			switch (type[1]) {
				case PC2_SPEED:
					ans.speed += modifs[0];
					break;
				case PC2_CD:
					ans.resp -= modifs[0];
					break;
				case PC2_COST:
					ans.price -= modifs[0];
					break;
				case PC2_HB:
					ans.hb += modifs[0];
					break;
				case PC2_TBA:
					ans.tba = (int) (ans.tba * (100 - modifs[0]) / 100.0);
			}
		}
	}

	public double getAtkMultiplication(int[] talents) {
		for(int i = 0; i < data.size(); i++) {
			if(data.get(i)[0] >= PC_CORRES.length || talents[i] == 0)
				continue;

			int[] type = PC_CORRES[data.get(i)[0]];
			if(type[0] == -1)
				continue;

			if(type[1] == PC2_ATK) {
				int maxlv = data.get(i)[1];
				int[] modifs = new int[4];
				if (maxlv > 1) {
					for (int j = 0; j < modifs.length; j++) {
						int v0 = modifiers[i][j][0];
						int v1 = modifiers[i][j][1];
						modifs[j] = (v1 - v0) * (talents[i] - 1) / (maxlv - 1) + v0;
					}
				}
				if (maxlv == 0)
					for (int j = 0; j < modifs.length; j++)
						modifs[j] = modifiers[i][j][0];

				return 1 + modifs[0] * 0.01;
			}
		}

		return 1.0;
	}

	public double getHPMultiplication(int[] talents) {
		for(int i = 0; i < data.size(); i++) {
			if(data.get(i)[0] >= PC_CORRES.length)
				continue;

			if(talents[i] == 0)
				continue;

			int[] type = PC_CORRES[data.get(i)[0]];

			if(type[0] == -1)
				continue;

			if(type[0] == PC_BASE && type[1] == PC2_HP) {
				int maxlv = data.get(i)[1];
				int[] modifs = new int[4];
				if (maxlv > 1) {
					for (int j = 0; j < modifs.length; j++) {
						int v0 = modifiers[i][j][0];
						int v1 = modifiers[i][j][1];
						modifs[j] = (v1 - v0) * (talents[i] - 1) / (maxlv - 1) + v0;
					}
				}
				if (maxlv == 0)
					for (int j = 0; j < modifs.length; j++)
						modifs[j] = modifiers[i][j][0];

				return 1 + modifs[0] * 0.01;
			}
		}

		return 1.0;
	}

	public int getTalentCount() {
		return data.size();
	}
	
	@OnInjected
	public void onInjected() {
		data.replaceAll(data -> {
			if (data.length != 14) {
				int[] newData = new int[14];
				System.arraycopy(data, 0, newData, 0, data.length);
				return newData;
			}
			return data;
		});
		info.removeIf(d -> d[0] > PCOIN_MAX);
	}

	private static boolean talentExist(String[] data, int index) {
		for(int i = index; i < index + 14; i++) {
			if (i >= data.length) {
				return false;
			}

			if(!data[i].trim().equals("0") && !data[i].trim().equals("-1")) {
				return true;
			}
		}

		return false;
	}
}
