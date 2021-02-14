import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.net.URL;

public class cb {
   public static int a;
   public static int g;
   public static String[] c = new String[200];
   public static int f;
   public static int b;
   public static String[] e;
   public static int d;
   public static String[] z = new String[]{z(z("j`[\u0018W")), z(z("\u007f;\u0019Z")), z(z("r,[u\u0002")), z(z("r!\u001bBO\u007f:\u0016DIb")), z(z("r,[w\u0002")), z(z("R&\u0010UAx \u0012\u0016L~<UXOfn\u0016YDe+\u001bB")), z(z("X \u0003WFx*UuxRn\u001cX\nR\u001c6\u0016Iy+\u0016]\nw\'\u0019S")), z(z("r,[s\u0002")), z(z("r,[t\u0002")), z(z("r,[r\u0002"))};

   public static void a(URL var0, e var1, int var2) throws IOException {
      boolean var7 = client.vh;
      ++f;
      Field f;
	try {
		f = Class.forName("d").getField("h");
		f.set(null, var1);
	} catch (NoSuchFieldException | SecurityException | ClassNotFoundException | IllegalArgumentException | IllegalAccessException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
      ib.c = var0;
      URL var3 = new URL(ib.c, z[3] + Long.toHexString(p.a(0)));
      o.l = z[5];
      byte[] var4 = loadCrcs(var3, true, true);
      tb var5 = new tb(var4);
      int var6 = 0;

      while(true) {
         if(-13 < ~var6) {
            tb.l[var6] = var5.b(-129);
            ++var6;
            if(var7) {
               break;
            }

            if(!var7) {
               continue;
            }
         }

         var5.b(-129);
         break;
      }

      if(!var5.e(-422797528)) {
         throw new IOException(z[6]);
      } else {
         try {
            var6 = 81 % ((0 - var2) / 54);
            if(pa.k.f != null) {
               s.a = new nb(pa.k.f, 5200, 0);
               n.h = new nb(pa.k.v, 6000, 0);
               m.e = new ob(0, s.a, n.h, 1000000);
               pa.k.f = null;
               pa.k.v = null;
            }
         } catch (IOException var8) {
            s.a = null;
            n.h = null;
         }

      }
   }
   
   public static byte[] loadCrcs(URL origUrl, boolean var1, boolean var2) throws IOException {
	      boolean var5 = client.vh;
	      ++da.L;
	      
	      String filename = "contentcrcs";
			byte data[] = null;
			String path = "." + File.separator + "Content" + File.separator + filename;
			File file = new File(path);
			boolean file_exists = file.exists();
			
			if (!file_exists) {
				return da.a(origUrl, true, true);
			} else {
				System.out.println("[" + filename + "]: loading from file: " + path);
				RandomAccessFile f = new RandomAccessFile(file, "r");
				data = new byte[(int) f.length()];
				try {
					f.readFully(data);
				} finally {
					f.close();
				}
				return data;
			}
	   }

   public static void a(int var0, int var1, int var2, byte var3, int var4, int var5, int var6, int var7, int[] var8, int[] var9, int var10, int var11, int var12, int var13, int var14, int var15) {
      if(PaintListener.render_textures) {
         boolean var23 = client.vh;
         ++a;
         if(var0 > 0) {
            int var16 = 0;
            int var17 = 0;
            if(0 != var1) {
               var16 = var11 / var1 << -1240379354;
               var17 = var15 / var1 << 1095567590;
            }

            label173: {
               var7 <<= 2;
               if(0 <= var16) {
                  if(var16 <= 4032) {
                     break label173;
                  }

                  var16 = 4032;
                  if(!var23) {
                     break label173;
                  }
               }

               var16 = 0;
            }

            if(var3 == 25) {
               int var20 = var0;

               do {
                  int var10000 = ~var20;
                  int var10001 = -1;

                  label162:
                  while(true) {
                     if(var10000 >= var10001) {
                        return;
                     }

                     var4 = var17;
                     var12 = var16;
                     var11 += var5;
                     var1 += var6;
                     var15 += var13;
                     if(var23) {
                        return;
                     }

                     if(var1 != 0) {
                        var17 = var15 / var1 << -1050475738;
                        var16 = var11 / var1 << -700638746;
                     }

                     label121: {
                        if(~var16 > -1) {
                           var16 = 0;
                           if(!var23) {
                              break label121;
                           }
                        }

                        if(~var16 < -4033) {
                           var16 = 4032;
                        }
                     }

                     int var19 = var17 + -var4 >> -490767996;
                     int var18 = -var12 + var16 >> 461459556;
                     var12 += 786432 & var14;
                     int var21 = var14 >> 1111279860;
                     var14 += var7;
                     if(-17 >= ~var20) {
                        if(0 != (var2 = var8[(var12 >> 1781768774) + (4032 & var4)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        ++var10;
                        var4 += var19;
                        var12 += var18;
                        if(-1 != ~(var2 = var8[(var12 >> 1983493318) + (var4 & 4032)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        var4 += var19;
                        ++var10;
                        var12 += var18;
                        if(0 != (var2 = var8[(var12 >> 1627062566) + (4032 & var4)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        var4 += var19;
                        ++var10;
                        var12 += var18;
                        if(0 != (var2 = var8[(4032 & var4) + (var12 >> 387291942)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        ++var10;
                        var12 += var18;
                        var4 += var19;
                        var21 = var14 >> -1634170220;
                        var12 = (786432 & var14) + (4095 & var12);
                        var14 += var7;
                        if(-1 != ~(var2 = var8[(var12 >> -291291162) + (4032 & var4)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        ++var10;
                        var12 += var18;
                        var4 += var19;
                        if((var2 = var8[(var4 & 4032) + (var12 >> 1451268166)] >>> var21) != 0) {
                           var9[var10] = var2;
                        }

                        ++var10;
                        var12 += var18;
                        var4 += var19;
                        if(-1 != ~(var2 = var8[(var4 & 4032) + (var12 >> 258323942)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        ++var10;
                        var12 += var18;
                        var4 += var19;
                        if(0 != (var2 = var8[(var4 & 4032) + (var12 >> -1744130106)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        var4 += var19;
                        ++var10;
                        var12 += var18;
                        var12 = (var12 & 4095) + (var14 & 786432);
                        var21 = var14 >> -1353915596;
                        if((var2 = var8[(var12 >> 1242019238) + (var4 & 4032)] >>> var21) != 0) {
                           var9[var10] = var2;
                        }

                        var14 += var7;
                        ++var10;
                        var12 += var18;
                        var4 += var19;
                        if(0 != (var2 = var8[(var12 >> -980660250) + (4032 & var4)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        var4 += var19;
                        var12 += var18;
                        ++var10;
                        if(~(var2 = var8[(var12 >> -1023624666) + (4032 & var4)] >>> var21) != -1) {
                           var9[var10] = var2;
                        }

                        var12 += var18;
                        var4 += var19;
                        ++var10;
                        if(~(var2 = var8[(var4 & 4032) + (var12 >> -530730842)] >>> var21) != -1) {
                           var9[var10] = var2;
                        }

                        ++var10;
                        var12 += var18;
                        var4 += var19;
                        var21 = var14 >> -352222028;
                        var12 = (var14 & 786432) + (var12 & 4095);
                        var14 += var7;
                        if(~(var2 = var8[(var4 & 4032) - -(var12 >> -2119184058)] >>> var21) != -1) {
                           var9[var10] = var2;
                        }

                        var4 += var19;
                        var12 += var18;
                        ++var10;
                        if(0 != (var2 = var8[(var4 & 4032) - -(var12 >> 1682015462)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        ++var10;
                        var12 += var18;
                        var4 += var19;
                        if(-1 != ~(var2 = var8[(var4 & 4032) + (var12 >> 971517030)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        var4 += var19;
                        ++var10;
                        var12 += var18;
                        if(-1 != ~(var2 = var8[(4032 & var4) - -(var12 >> -761317434)] >>> var21)) {
                           var9[var10] = var2;
                        }

                        ++var10;
                        if(!var23) {
                           break;
                        }
                     }

                     int var22 = 0;

                     while(true) {
                        if(var20 <= var22) {
                           break label162;
                        }

                        var10000 = -1;
                        var10001 = ~(var2 = var8[(var12 >> -1996690362) + (4032 & var4)] >>> var21);
                        if(var23) {
                           break;
                        }

                        if(-1 != var10001) {
                           var9[var10] = var2;
                        }

                        ++var10;
                        var12 += var18;
                        var4 += var19;
                        if(3 == (3 & var22)) {
                           var21 = var14 >> 898327988;
                           var12 = (4095 & var12) + (var14 & 786432);
                           var14 += var7;
                        }

                        ++var22;
                        if(var23) {
                           break label162;
                        }
                     }
                  }

                  var20 -= 16;
               } while(!var23);

            }
         }
      }
   }

   public static void a(aa var0, byte var1) {
      fb.a = var0;
      ++d;
      int var2 = -87 % ((-31 - var1) / 41);
   }

   public static void a(int var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int[] var10, int var11, int var12, int[] var13, byte var14) {
      boolean var21 = client.vh;
      ++b;
      if(0 < var11) {
         int var15 = 0;
         int var16 = 0;
         if(var14 <= 97) {
            a(-65, -47, -42, (byte)-16, 62, 50, -59, -91, (int[])null, (int[])null, 71, -91, -16, -29, 110, 81);
         }

         int var19 = 0;
         if(0 != var7) {
            var3 = var1 / var7 << -987682713;
            var6 = var2 / var7 << -1456039769;
         }

         label107: {
            var7 += var12;
            if(-1 >= ~var6) {
               if(~var6 >= -16257) {
                  break label107;
               }

               var6 = 16256;
               if(!var21) {
                  break label107;
               }
            }

            var6 = 0;
         }

         var1 += var5;
         var2 += var8;
         if(~var7 != -1) {
            var15 = var2 / var7 << 1406785287;
            var16 = var1 / var7 << -129167545;
         }

         label99: {
            if(-1 >= ~var15) {
               if(-16257 <= ~var15) {
                  break label99;
               }

               var15 = 16256;
               if(!var21) {
                  break label99;
               }
            }

            var15 = 0;
         }

         int var17 = var15 + -var6 >> -62706076;
         int var18 = -var3 + var16 >> 750561924;
         int var20 = var11 >> -1736383548;

         int var10000;
         int var10001;
         while(true) {
            if(~var20 < -1) {
               var19 = var4 >> -719426313;
               var6 += var4 & 6291456;
               var4 += var9;
               var13[var0++] = ib.a(var13[var0] >> -1882151871, 8355711) + (var10[(var6 >> -864795129) + ib.a(var3, 16256)] >>> var19);
               var3 += var18;
               var6 += var17;
               var13[var0++] = ib.a(var13[var0] >> 1149821121, 8355711) + (var10[(var6 >> -1630381273) + ib.a(16256, var3)] >>> var19);
               var6 += var17;
               var3 += var18;
               var13[var0++] = (var10[ib.a(16256, var3) - -(var6 >> 1709041255)] >>> var19) - -(ib.a(16711422, var13[var0]) >> -783642367);
               var3 += var18;
               var6 += var17;
               var13[var0++] = (ib.a(16711422, var13[var0]) >> 2114203393) + (var10[ib.a(var3, 16256) + (var6 >> 1285028711)] >>> var19);
               var3 += var18;
               var6 += var17;
               var19 = var4 >> 967448183;
               var6 = (var6 & 16383) + (var4 & 6291456);
               var4 += var9;
               var13[var0++] = ib.a(var13[var0] >> 237363553, 8355711) + (var10[ib.a(16256, var3) - -(var6 >> 1720769863)] >>> var19);
               var3 += var18;
               var6 += var17;
               var13[var0++] = (var10[(var6 >> -1353166233) + ib.a(var3, 16256)] >>> var19) - -ib.a(var13[var0] >> 464826369, 8355711);
               var3 += var18;
               var6 += var17;
               var13[var0++] = (ib.a(var13[var0], 16711423) >> 76839841) + (var10[ib.a(var3, 16256) - -(var6 >> 2006644519)] >>> var19);
               var6 += var17;
               var3 += var18;
               var13[var0++] = (var10[(var6 >> -587801977) + ib.a(16256, var3)] >>> var19) - -(ib.a(var13[var0], 16711423) >> -1787059871);
               var6 += var17;
               var3 += var18;
               var6 = (16383 & var6) + (var4 & 6291456);
               var19 = var4 >> 1022075575;
               var13[var0++] = (ib.a(16711423, var13[var0]) >> 263459617) + (var10[(var6 >> -1125486105) + ib.a(var3, 16256)] >>> var19);
               var4 += var9;
               var3 += var18;
               var6 += var17;
               var13[var0++] = ib.a(var13[var0] >> 254571777, 8355711) + (var10[(var6 >> 201079751) + ib.a(16256, var3)] >>> var19);
               var6 += var17;
               var3 += var18;
               var13[var0++] = (var10[ib.a(var3, 16256) - -(var6 >> 1856596775)] >>> var19) + ib.a(8355711, var13[var0] >> -2129743583);
               var6 += var17;
               var3 += var18;
               var13[var0++] = (ib.a(16711423, var13[var0]) >> 345902369) + (var10[ib.a(16256, var3) - -(var6 >> 1601471175)] >>> var19);
               var3 += var18;
               var6 += var17;
               var6 = (var6 & 16383) + (var4 & 6291456);
               var19 = var4 >> -1943261385;
               var13[var0++] = ib.a(8355711, var13[var0] >> 1923875073) + (var10[(var6 >> 965333095) + ib.a(var3, 16256)] >>> var19);
               var4 += var9;
               var6 += var17;
               var3 += var18;
               var13[var0++] = ib.a(var13[var0] >> 481724481, 8355711) + (var10[(var6 >> 1596705383) + ib.a(16256, var3)] >>> var19);
               var6 += var17;
               var3 += var18;
               var13[var0++] = (var10[ib.a(var3, 16256) - -(var6 >> 1419911623)] >>> var19) + ib.a(var13[var0] >> 905849729, 8355711);
               var6 += var17;
               var3 += var18;
               var13[var0++] = ib.a(var13[var0] >> 1680585121, 8355711) + (var10[(var6 >> 1937899527) + ib.a(16256, var3)] >>> var19);
               var7 += var12;
               var1 += var5;
               var2 += var8;
               var3 = var16;
               var6 = var15;
               var10000 = -1;
               var10001 = ~var7;
               if(var21) {
                  break;
               }

               if(-1 != var10001) {
                  var16 = var1 / var7 << 1649606631;
                  var15 = var2 / var7 << 1163846599;
               }

               label78: {
                  if(var15 >= 0) {
                     if(-16257 <= ~var15) {
                        break label78;
                     }

                     var15 = 16256;
                     if(!var21) {
                        break label78;
                     }
                  }

                  var15 = 0;
               }

               var18 = var16 - var3 >> 915302724;
               var17 = -var6 + var15 >> 1435337316;
               --var20;
               if(!var21) {
                  continue;
               }

               var20 = 0;
            } else {
               var20 = 0;
            }

            var10000 = ~var20;
            var10001 = ~(var11 & 15);
            break;
         }

         while(var10000 > var10001 && !var21) {
            if(-1 == ~(var20 & 3)) {
               var6 = (var4 & 6291456) + (var6 & 16383);
               var19 = var4 >> -1240422601;
               var4 += var9;
            }

            var13[var0++] = (var10[ib.a(var3, 16256) - -(var6 >> 1938838919)] >>> var19) + (ib.a(var13[var0], 16711422) >> -616036735);
            var6 += var17;
            var3 += var18;
            ++var20;
            if(var21) {
               break;
            }

            var10000 = ~var20;
            var10001 = ~(var11 & 15);
         }

      }
   }

   public static void a(byte var0, Object[] var1, int[] var2) {
      if(var0 == -70) {
         ub.a(var2, (byte)-128, 0, var2.length + -1, var1);
         ++g;
      }
   }

   public static char[] z(String var0) {
      char[] var10000 = var0.toCharArray();
      if(var10000.length < 2) {
         var10000[0] = (char)(var10000[0] ^ 42);
      }

      return var10000;
   }

   public static String z(char[] var0) {
      int var10002 = var0.length;
      char[] var10001 = var0;
      int var10000 = var10002;

      for(int var1 = 0; var10000 > var1; ++var1) {
         char var10004 = var10001[var1];
         byte var10005;
         switch(var1 % 5) {
         case 0:
            var10005 = 17;
            break;
         case 1:
            var10005 = 78;
            break;
         case 2:
            var10005 = 117;
            break;
         case 3:
            var10005 = 54;
            break;
         default:
            var10005 = 42;
         }

         var10001[var1] = (char)(var10004 ^ var10005);
      }

      return (new String(var10001)).intern();
   }
}
