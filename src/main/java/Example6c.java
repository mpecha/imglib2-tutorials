/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * An exception is the 1D FFT implementation of Dave Hale which we use as a
 * library, which is released under the terms of the Common Public License -
 * v1.0, which is available at http://www.eclipse.org/legal/cpl-v10.html
 */
import ij.ImageJ;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import net.imglib2.algorithm.fft.FourierConvolution;
import net.imglib2.algorithm.fft.FourierTransform;
import net.imglib2.algorithm.fft.InverseFourierTransform;
import net.imglib2.converter.ComplexImaginaryFloatConverter;
import net.imglib2.converter.ComplexPhaseFloatConverter;
import net.imglib2.converter.ComplexRealFloatConverter;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.RealSum;

/**
 * Perform template matching by convolution in the Fourier domain
 *
 * @author Stephan Preibisch
 * @author Stephan Saalfeld
 */
public class Example6c
{
	public Example6c() throws ImgIOException, IncompatibleTypeException
	{
		// open with ImgOpener as FloatTypes
		final Img< FloatType > image = new ImgOpener().openImg( "DrosophilaWing.tif",
			new FloatType() );
		final Img< FloatType > template = new ImgOpener().openImg( "WingTemplate.tif",
			new FloatType() );

		// display image and template
		ImageJFunctions.show( image ).setTitle( "input" );
		ImageJFunctions.show( template ).setTitle( "template" );

		// compute fourier transform of the template
		final FourierTransform< FloatType, ComplexFloatType > fft =
			new FourierTransform< FloatType, ComplexFloatType >(
				template, new ComplexFloatType() );
		fft.process();
		final Img< ComplexFloatType > templateFFT = fft.getResult();

		// display fft (by default in generalized log power spectrum
		ImageJFunctions.show( templateFFT ).setTitle( "fft power spectrum" );
		// display fft phase spectrum
		ImageJFunctions.show( templateFFT,
			new ComplexPhaseFloatConverter< ComplexFloatType >() )
				.setTitle( "fft phase spectrum" );
		// display fft real values
		ImageJFunctions.show( templateFFT,
			new ComplexRealFloatConverter< ComplexFloatType >() )
				.setTitle( "fft real values" );
		// display fft imaginary values
		ImageJFunctions.show( templateFFT,
			new ComplexImaginaryFloatConverter< ComplexFloatType >() )
				.setTitle( "fft imaginary values" );

		// complex invert the kernel
		final ComplexFloatType c = new ComplexFloatType();
		for ( final ComplexFloatType t : templateFFT )
		{
			c.set( t );
			t.complexConjugate();
			c.mul( t );
			t.div( c );
		}

		// compute inverse fourier transform of the template
		final InverseFourierTransform< FloatType, ComplexFloatType > ifft =
			new InverseFourierTransform< FloatType, ComplexFloatType >( templateFFT, fft );
		ifft.process();
		final Img< FloatType > templateInverse = ifft.getResult();

		// display the inverse template
		ImageJFunctions.show( templateInverse ).setTitle( "inverse template" );

		// normalize the inverse template
		norm( templateInverse );

		// compute fourier convolution of the inverse template and the image and display it
		ImageJFunctions.show( FourierConvolution.convolve( image, templateInverse ) );
	}
	
	/**
	 * Computes the sum of all pixels in an iterable using RealSum
	 *
	 * @param iterable - the image data
	 * @return - the sum of values
	 */
	public static < T extends RealType< T > > double sumImage( final Iterable< T > iterable )
	{
		final RealSum sum = new RealSum();

		for ( final T type : iterable )
			sum.add( type.getRealDouble() );

		return sum.getSum();
	}

	/**
	 * Norms all image values so that their sum is 1
	 *
	 * @param iterable - the image data
	 */
	public static void norm( final Iterable< FloatType > iterable )
	{
		final double sum = sumImage( iterable );

		for ( final FloatType type : iterable )
			type.setReal( type.get() / sum );
	}

	public static void main( final String[] args ) throws ImgIOException, IncompatibleTypeException
	{
		// open an ImageJ window
		new ImageJ();

		// run the example
		new Example6c();
	}
}
