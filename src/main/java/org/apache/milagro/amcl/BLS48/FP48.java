/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

/* AMCL Fp^48 functions */
/* FP48 elements are of the form a+i.b+i^2.c */

package org.apache.milagro.amcl.BLS48;

public final class FP48 {
	private final FP16 a;
	private final FP16 b;
	private final FP16 c;
/* reduce all components of this mod Modulus */
	public void reduce()
	{
		a.reduce();
		b.reduce();
		c.reduce();
	}

/* normalise all components of this */
	public void norm()
	{
		a.norm();
		b.norm();
		c.norm();
	}
/* test x==0 ? */
	public boolean iszilch() {
		//reduce();
		return (a.iszilch() && b.iszilch() && c.iszilch());
	}

	public void cmove(FP48 g,int d)
	{
		a.cmove(g.a,d);
		b.cmove(g.b,d);
		c.cmove(g.c,d);		
	}


/* return 1 if b==c, no branching */
	public static int teq(int b,int c)
	{
		int x=b^c;
		x-=1;  // if x=0, x now -1
		return ((x>>31)&1);
	}

/* Constant time select from pre-computed table */
	public void select(FP48 g[],int b)
	{
		int m=b>>31;
		int babs=(b^m)-m;

		babs=(babs-1)/2;

		cmove(g[0],teq(babs,0));  // conditional move
		cmove(g[1],teq(babs,1));
		cmove(g[2],teq(babs,2));
		cmove(g[3],teq(babs,3));
		cmove(g[4],teq(babs,4));
		cmove(g[5],teq(babs,5));
		cmove(g[6],teq(babs,6));
		cmove(g[7],teq(babs,7));
 
		FP48 invf=new FP48(this); 
		invf.conj();
		cmove(invf,(int)(m&1));
	}

	/* test x==1 ? */
	public boolean isunity() {
		FP16 one=new FP16(1);
		return (a.equals(one) && b.iszilch() && c.iszilch());
	}
/* return 1 if x==y, else 0 */
	public boolean equals(FP48 x)
	{
		return (a.equals(x.a) && b.equals(x.b) && c.equals(x.c));
	}
/* extract a from this */
	public FP16 geta()
	{
		return a;
	}
/* extract b */
	public FP16 getb()
	{
		return b;
	}
/* extract c */
	public FP16 getc()
	{
		return c;
	}
/* copy this=x */
	public void copy(FP48 x)
	{
		a.copy(x.a);
		b.copy(x.b);
		c.copy(x.c);
	}
/* set this=1 */
	public void one()
	{
		a.one();
		b.zero();
		c.zero();
	}
/* this=conj(this) */
	public void conj()
	{
		a.conj();
		b.nconj();
		c.conj();
	}
/* Constructors */
	public FP48(FP16 d)
	{
		a=new FP16(d);
		b=new FP16(0);
		c=new FP16(0);
	}

	public FP48(int d)
	{
		a=new FP16(d);
		b=new FP16(0);
		c=new FP16(0);
	}

	public FP48(FP16 d,FP16 e,FP16 f)
	{
		a=new FP16(d);
		b=new FP16(e);
		c=new FP16(f);
	}

	public FP48(FP48 x)
	{
		a=new FP16(x.a);
		b=new FP16(x.b);
		c=new FP16(x.c);
	}

/* Granger-Scott Unitary Squaring */
	public void usqr()
	{
		FP16 A=new FP16(a);
		FP16 B=new FP16(c);
		FP16 C=new FP16(b);
		FP16 D=new FP16(0);

		a.sqr();
		D.copy(a); D.add(a);
		a.add(D);

		a.norm();
		A.nconj();

		A.add(A);
		a.add(A);
		B.sqr();
		B.times_i();

		D.copy(B); D.add(B);
		B.add(D);
		B.norm();

		C.sqr();
		D.copy(C); D.add(C);
		C.add(D);
		C.norm();

		b.conj();
		b.add(b);
		c.nconj();

		c.add(c);
		b.add(B);
		c.add(C);
		reduce();
	}

/* Chung-Hasan SQR2 method from http://cacr.uwaterloo.ca/techreports/2006/cacr2006-24.pdf */
	public void sqr()
	{
		FP16 A=new FP16(a);
		FP16 B=new FP16(b);
		FP16 C=new FP16(c);
		FP16 D=new FP16(a);

		A.sqr();
		B.mul(c);
		B.add(B);
		B.norm();
		C.sqr();
		D.mul(b);
		D.add(D);

		c.add(a);
		c.add(b);
		c.norm();
		c.sqr();

		a.copy(A);

		A.add(B);
		A.norm();
		A.add(C);
		A.add(D);
		A.norm();

		A.neg();
		B.times_i();
		C.times_i();

		a.add(B);

		b.copy(C); b.add(D);
		c.add(A);

		norm();
	}

/* FP12 full multiplication this=this*y */
	public void mul(FP48 y)
	{
		FP16 z0=new FP16(a);
		FP16 z1=new FP16(0);
		FP16 z2=new FP16(b);
		FP16 z3=new FP16(0);
		FP16 t0=new FP16(a);
		FP16 t1=new FP16(y.a);

		z0.mul(y.a);
		z2.mul(y.b);

		t0.add(b);
		t1.add(y.b);

		t0.norm();
		t1.norm();

		z1.copy(t0); z1.mul(t1);
		t0.copy(b); t0.add(c);

		t1.copy(y.b); t1.add(y.c);

		t0.norm();
		t1.norm();

		z3.copy(t0); z3.mul(t1);

		t0.copy(z0); t0.neg();
		t1.copy(z2); t1.neg();

		z1.add(t0);
		//z1.norm();
		b.copy(z1); b.add(t1);

		z3.add(t1);
		z2.add(t0);

		t0.copy(a); t0.add(c);
		t1.copy(y.a); t1.add(y.c);

		t0.norm();
		t1.norm();
	
		t0.mul(t1);
		z2.add(t0);

		t0.copy(c); t0.mul(y.c);
		t1.copy(t0); t1.neg();

		c.copy(z2); c.add(t1);
		z3.add(t1);
		t0.times_i();
		b.add(t0);
		z3.norm();
		z3.times_i();
		a.copy(z0); a.add(z3);
		norm();

	}

/* Special case of multiplication arises from special form of ATE pairing line function */
	public void smul(FP48 y,int type)
	{
		if (type==ECP.D_TYPE)
		{
			FP16 z0=new FP16(a);
			FP16 z2=new FP16(b);
			FP16 z3=new FP16(b);
			FP16 t0=new FP16(0);
			FP16 t1=new FP16(y.a);
			z0.mul(y.a);
			z2.pmul(y.b.real());
			b.add(a);
			t1.real().add(y.b.real());

			t1.norm();
			b.norm();
			b.mul(t1);
			z3.add(c);
			z3.norm();
			z3.pmul(y.b.real());

			t0.copy(z0); t0.neg();
			t1.copy(z2); t1.neg();

			b.add(t0);

			b.add(t1);
			z3.add(t1);
			z2.add(t0);

			t0.copy(a); t0.add(c);
			t0.norm();
			z3.norm();
			t0.mul(y.a);
			c.copy(z2); c.add(t0);

			z3.times_i();
			a.copy(z0); a.add(z3);
		}
		if (type==ECP.M_TYPE)
		{
			FP16 z0=new FP16(a);
			FP16 z1=new FP16(0);
			FP16 z2=new FP16(0);
			FP16 z3=new FP16(0);
			FP16 t0=new FP16(a);
			FP16 t1=new FP16(0);
		
			z0.mul(y.a);
			t0.add(b);
			t0.norm();

			z1.copy(t0); z1.mul(y.a);
			t0.copy(b); t0.add(c);
			t0.norm();

			z3.copy(t0); //z3.mul(y.c);
			z3.pmul(y.c.getb());
			z3.times_i();

			t0.copy(z0); t0.neg();

			z1.add(t0);
			b.copy(z1); 
			z2.copy(t0);

			t0.copy(a); t0.add(c);
			t1.copy(y.a); t1.add(y.c);

			t0.norm();
			t1.norm();
	
			t0.mul(t1);
			z2.add(t0);

			t0.copy(c); 
			
			t0.pmul(y.c.getb());
			t0.times_i();

			t1.copy(t0); t1.neg();

			c.copy(z2); c.add(t1);
			z3.add(t1);
			t0.times_i();
			b.add(t0);
			z3.norm();
			z3.times_i();
			a.copy(z0); a.add(z3);
		}
		norm();
	}

/* this=1/this */
	public void inverse()
	{
		FP16 f0=new FP16(a);
		FP16 f1=new FP16(b);
		FP16 f2=new FP16(a);
		FP16 f3=new FP16(0);

		norm();
		f0.sqr();
		f1.mul(c);
		f1.times_i();
		f0.sub(f1);
		f0.norm();

		f1.copy(c); f1.sqr();
		f1.times_i();
		f2.mul(b);
		f1.sub(f2);
		f1.norm();

		f2.copy(b); f2.sqr();
		f3.copy(a); f3.mul(c);
		f2.sub(f3);
		f2.norm();

		f3.copy(b); f3.mul(f2);
		f3.times_i();
		a.mul(f0);
		f3.add(a);
		c.mul(f1);
		c.times_i();

		f3.add(c);
		f3.norm();
		f3.inverse();
		a.copy(f0); a.mul(f3);
		b.copy(f1); b.mul(f3);
		c.copy(f2); c.mul(f3);
	}

/* this=this^p using Frobenius */
	public void frob(FP2 f,int n)
	{
		FP2 f2=new FP2(f);
		FP2 f3=new FP2(f);

		f2.sqr();
		f3.mul(f2);

		f3.mul_ip(); f3.norm();
		f3.mul_ip(); f3.norm();

		for (int i=0;i<n;i++)
		{
			a.frob(f3);
			b.frob(f3);
			c.frob(f3);

			b.qmul(f); b.times_i4(); b.times_i2(); 
			c.qmul(f2); c.times_i4(); c.times_i4(); c.times_i4(); 
		}
	}

/* trace function */
	public FP16 trace()
	{
		FP16 t=new FP16(0);
		t.copy(a);
		t.imul(3);
		t.reduce();
		return t;
	}

/* convert from byte array to FP12 */
	public static FP48 fromBytes(byte[] w)
	{
		BIG a,b;
		FP2 c,d;
		FP4 ea,eb;
		FP8 fa,fb;
		FP16 e,f,g;
		byte[] t=new byte[BIG.MODBYTES];

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+2*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+3*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		ea=new FP4(c,d);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+4*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+5*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+6*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+7*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		eb=new FP4(c,d);

		fa=new FP8(ea,eb);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+8*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+9*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+10*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+11*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		ea=new FP4(c,d);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+12*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+13*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+14*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+15*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		eb=new FP4(c,d);

		fb=new FP8(ea,eb);

		e=new FP16(fa,fb);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+16*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+17*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+18*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+19*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		ea=new FP4(c,d);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+20*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+21*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+22*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+23*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		eb=new FP4(c,d);

		fa=new FP8(ea,eb);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+24*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+25*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+26*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+27*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		ea=new FP4(c,d);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+28*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+29*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+30*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+31*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		eb=new FP4(c,d);

		fb=new FP8(ea,eb);

		f=new FP16(fa,fb);


		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+32*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+33*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+34*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+35*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		ea=new FP4(c,d);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+36*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+37*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+38*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+39*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		eb=new FP4(c,d);

		fa=new FP8(ea,eb);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+40*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+41*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+42*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+43*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		ea=new FP4(c,d);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+44*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+45*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		c=new FP2(a,b);

		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+46*BIG.MODBYTES];
		a=BIG.fromBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) t[i]=w[i+47*BIG.MODBYTES];
		b=BIG.fromBytes(t);
		d=new FP2(a,b);

		eb=new FP4(c,d);

		fb=new FP8(ea,eb);

		g=new FP16(fa,fb);

		return new FP48(e,f,g);
	}

/* convert this to byte array */
	public void toBytes(byte[] w)
	{
		byte[] t=new byte[BIG.MODBYTES];

		a.geta().geta().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i]=t[i];
		a.geta().geta().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+BIG.MODBYTES]=t[i];
		a.geta().geta().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+2*BIG.MODBYTES]=t[i];
		a.geta().geta().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+3*BIG.MODBYTES]=t[i];

		a.geta().getb().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+4*BIG.MODBYTES]=t[i];
		a.geta().getb().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+5*BIG.MODBYTES]=t[i];
		a.geta().getb().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+6*BIG.MODBYTES]=t[i];
		a.geta().getb().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+7*BIG.MODBYTES]=t[i];
		a.getb().geta().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+8*BIG.MODBYTES]=t[i];
		a.getb().geta().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+9*BIG.MODBYTES]=t[i];
		a.getb().geta().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+10*BIG.MODBYTES]=t[i];
		a.getb().geta().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+11*BIG.MODBYTES]=t[i];
		a.getb().getb().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+12*BIG.MODBYTES]=t[i];
		a.getb().getb().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+13*BIG.MODBYTES]=t[i];
		a.getb().getb().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+14*BIG.MODBYTES]=t[i];
		a.getb().getb().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+15*BIG.MODBYTES]=t[i];

		b.geta().geta().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+16*BIG.MODBYTES]=t[i];
		b.geta().geta().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+17*BIG.MODBYTES]=t[i];
		b.geta().geta().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+18*BIG.MODBYTES]=t[i];
		b.geta().geta().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+19*BIG.MODBYTES]=t[i];

		b.geta().getb().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+20*BIG.MODBYTES]=t[i];
		b.geta().getb().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+21*BIG.MODBYTES]=t[i];
		b.geta().getb().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+22*BIG.MODBYTES]=t[i];
		b.geta().getb().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+23*BIG.MODBYTES]=t[i];
		b.getb().geta().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+24*BIG.MODBYTES]=t[i];
		b.getb().geta().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+25*BIG.MODBYTES]=t[i];
		b.getb().geta().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+26*BIG.MODBYTES]=t[i];
		b.getb().geta().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+27*BIG.MODBYTES]=t[i];
		b.getb().getb().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+28*BIG.MODBYTES]=t[i];
		b.getb().getb().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+29*BIG.MODBYTES]=t[i];
		b.getb().getb().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+30*BIG.MODBYTES]=t[i];
		b.getb().getb().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+31*BIG.MODBYTES]=t[i];


		c.geta().geta().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+32*BIG.MODBYTES]=t[i];
		c.geta().geta().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+33*BIG.MODBYTES]=t[i];
		c.geta().geta().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+34*BIG.MODBYTES]=t[i];
		c.geta().geta().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+35*BIG.MODBYTES]=t[i];
		c.geta().getb().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+36*BIG.MODBYTES]=t[i];
		c.geta().getb().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+37*BIG.MODBYTES]=t[i];
		c.geta().getb().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+38*BIG.MODBYTES]=t[i];
		c.geta().getb().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+39*BIG.MODBYTES]=t[i];
		c.getb().geta().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+40*BIG.MODBYTES]=t[i];
		c.getb().geta().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+41*BIG.MODBYTES]=t[i];
		c.getb().geta().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+42*BIG.MODBYTES]=t[i];
		c.getb().geta().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+43*BIG.MODBYTES]=t[i];
		c.getb().getb().geta().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+44*BIG.MODBYTES]=t[i];
		c.getb().getb().geta().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+45*BIG.MODBYTES]=t[i];
		c.getb().getb().getb().getA().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+46*BIG.MODBYTES]=t[i];
		c.getb().getb().getb().getB().toBytes(t);
		for (int i=0;i<BIG.MODBYTES;i++) w[i+47*BIG.MODBYTES]=t[i];
	}

/* convert to hex string */
	public String toString() 
	{
		return ("["+a.toString()+","+b.toString()+","+c.toString()+"]");
	}

/* this=this^e */ 
/* Note this is simple square and multiply, so not side-channel safe */
	public FP48 pow(BIG e)
	{
		norm();
		e.norm();
		BIG e3=new BIG(e);
		e3.pmul(3);
		e3.norm();

		FP48 w=new FP48(this);

		int nb=e3.nbits();
		for (int i=nb-2;i>=1;i--)
		{
			w.usqr();
			int bt=e3.bit(i)-e.bit(i);
			if (bt==1)
				w.mul(this);
			if (bt==-1)
			{
				conj(); w.mul(this); conj();
			}
		}
		w.reduce();
		return w;

	}

/* constant time powering by small integer of max length bts */
	public void pinpow(int e,int bts)
	{
		int i,b;
		FP48 [] R=new FP48[2];
		R[0]=new FP48(1);
		R[1]=new FP48(this);
		for (i=bts-1;i>=0;i--)
		{
			b=(e>>i)&1;
			R[1-b].mul(R[b]);
			R[b].usqr();
		}
		this.copy(R[0]);
	}

	public FP16 compow(BIG e,BIG r)
	{
		FP48 g1=new FP48(0);
		FP48 g2=new FP48(0);
		FP2 f=new FP2(new BIG(ROM.Fra),new BIG(ROM.Frb));
		BIG q=new BIG(ROM.Modulus);

		BIG m=new BIG(q);
		m.mod(r);

		BIG a=new BIG(e);
		a.mod(m);

		BIG b=new BIG(e);
		b.div(m);

		g1.copy(this);
		g2.copy(this);

		FP16 c=g1.trace();

		if (b.iszilch())
		{
			c=c.xtr_pow(e);
			return c;
		}

		g2.frob(f,1);
		FP16 cp=g2.trace();
		g1.conj();
		g2.mul(g1);
		FP16 cpm1=g2.trace();
		g2.mul(g1);
		FP16 cpm2=g2.trace();

		c=c.xtr_pow2(cp,cpm1,cpm2,a,b);

		return c;
	}

/* p=q0^u0.q1^u1.q2^u2.q3^u3.... */
// Bos & Costello https://eprint.iacr.org/2013/458.pdf
// Faz-Hernandez & Longa & Sanchez  https://eprint.iacr.org/2013/158.pdf
// Side channel attack secure 

	public static FP48 pow16(FP48[] q,BIG[] u)
	{
		int i,j,k,nb,pb1,pb2,pb3,pb4;
		FP48 [] g1=new FP48[8];
		FP48 [] g2=new FP48[8];
		FP48 [] g3=new FP48[8];
		FP48 [] g4=new FP48[8];
		FP48 r=new FP48(1);
		FP48 p=new FP48(0);
		BIG [] t=new BIG[16];
		BIG mt=new BIG(0);
		byte[] w1=new byte[BIG.NLEN*BIG.BASEBITS+1];
		byte[] s1=new byte[BIG.NLEN*BIG.BASEBITS+1];
		byte[] w2=new byte[BIG.NLEN*BIG.BASEBITS+1];
		byte[] s2=new byte[BIG.NLEN*BIG.BASEBITS+1];
		byte[] w3=new byte[BIG.NLEN*BIG.BASEBITS+1];
		byte[] s3=new byte[BIG.NLEN*BIG.BASEBITS+1];
		byte[] w4=new byte[BIG.NLEN*BIG.BASEBITS+1];
		byte[] s4=new byte[BIG.NLEN*BIG.BASEBITS+1];

		for (i=0;i<16;i++)
		{
			t[i]=new BIG(u[i]);
			t[i].norm();
		}

		g1[0]=new FP48(q[0]);  // q[0]
		g1[1]=new FP48(g1[0]); g1[1].mul(q[1]); // q[0].q[1]
		g1[2]=new FP48(g1[0]); g1[2].mul(q[2]); // q[0].q[2]
		g1[3]=new FP48(g1[1]); g1[3].mul(q[2]); // q[0].q[1].q[2]
		g1[4]=new FP48(q[0]);  g1[4].mul(q[3]); // q[0].q[3]
		g1[5]=new FP48(g1[1]); g1[5].mul(q[3]); // q[0].q[1].q[3]
		g1[6]=new FP48(g1[2]); g1[6].mul(q[3]); // q[0].q[2].q[3]
		g1[7]=new FP48(g1[3]); g1[7].mul(q[3]); // q[0].q[1].q[2].q[3]

// Use Frobenius
		FP2 f=new FP2(new BIG(ROM.Fra),new BIG(ROM.Frb));
		for (i=0;i<8;i++)
		{
			g2[i]=new FP48(g1[i]);
			g2[i].frob(f,4);
			g3[i]=new FP48(g2[i]);
			g3[i].frob(f,4);
			g4[i]=new FP48(g3[i]);
			g4[i].frob(f,4);
		}

    // Make it odd
        pb1=1-t[0].parity();
        t[0].inc(pb1);
        t[0].norm();

        pb2=1-t[4].parity();
        t[4].inc(pb2);
        t[4].norm();

        pb3=1-t[8].parity();
        t[8].inc(pb3);
        t[8].norm();

        pb4=1-t[12].parity();
        t[12].inc(pb4);
        t[12].norm();


    // Number of bits
        mt.zero();
        for (i=0;i<16;i++) {
            mt.or(t[i]);
        }
        nb=1+mt.nbits();

    // Sign pivot 
        s1[nb-1]=1;
		s2[nb-1]=1;
        s3[nb-1]=1;
		s4[nb-1]=1;
        for (i=0;i<nb-1;i++) {
            t[0].fshr(1);
            s1[i]=(byte)(2*t[0].parity()-1);
            t[4].fshr(1);
            s2[i]=(byte)(2*t[4].parity()-1);
            t[8].fshr(1);
            s3[i]=(byte)(2*t[8].parity()-1);
            t[12].fshr(1);
            s4[i]=(byte)(2*t[12].parity()-1);
 
        }

    // Recoded exponent
        for (i=0; i<nb; i++) {
            w1[i]=0;
            k=1;
            for (j=1; j<4; j++) {
                byte bt=(byte)(s1[i]*t[j].parity());
                t[j].fshr(1);
                t[j].dec((int)(bt)>>1);
                t[j].norm();
                w1[i]+=bt*(byte)k;
                k*=2;
            }

            w2[i]=0;
            k=1;
            for (j=5; j<8; j++) {
                byte bt=(byte)(s2[i]*t[j].parity());
                t[j].fshr(1);
                t[j].dec((int)(bt)>>1);
                t[j].norm();
                w2[i]+=bt*(byte)k;
                k*=2;
            }

            w3[i]=0;
            k=1;
            for (j=9; j<12; j++) {
                byte bt=(byte)(s3[i]*t[j].parity());
                t[j].fshr(1);
                t[j].dec((int)(bt)>>1);
                t[j].norm();
                w3[i]+=bt*(byte)k;
                k*=2;
            }

            w4[i]=0;
            k=1;
            for (j=13; j<16; j++) {
                byte bt=(byte)(s4[i]*t[j].parity());
                t[j].fshr(1);
                t[j].dec((int)(bt)>>1);
                t[j].norm();
                w4[i]+=bt*(byte)k;
                k*=2;
            }

        } 


     // Main loop
        p.select(g1,(int)(2*w1[nb-1]+1)); 
		r.select(g2,(int)(2*w2[nb-1]+1)); 
		p.mul(r);
		r.select(g3,(int)(2*w3[nb-1]+1)); 
		p.mul(r);
		r.select(g4,(int)(2*w4[nb-1]+1)); 
		p.mul(r);

        for (i=nb-2;i>=0;i--) {
            p.usqr();
            r.select(g1,(int)(2*w1[i]+s1[i]));
            p.mul(r);
            r.select(g2,(int)(2*w2[i]+s2[i]));
            p.mul(r);
            r.select(g3,(int)(2*w3[i]+s3[i]));
            p.mul(r);
            r.select(g4,(int)(2*w4[i]+s4[i]));
            p.mul(r);

        }

    // apply correction
        r.copy(q[0]); r.conj();   
        r.mul(p);
        p.cmove(r,pb1);

        r.copy(q[4]); r.conj();   
        r.mul(p);
        p.cmove(r,pb2);

        r.copy(q[8]); r.conj();   
        r.mul(p);
        p.cmove(r,pb3);

        r.copy(q[12]); r.conj();   
        r.mul(p);
        p.cmove(r,pb4);

 		p.reduce();
		return p;
	}              
}
