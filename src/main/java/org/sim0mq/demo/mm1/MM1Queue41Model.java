package org.sim0mq.demo.mm1;

import java.io.Serializable;
import java.rmi.RemoteException;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.dsol.formalisms.Resource;
import nl.tudelft.simulation.dsol.formalisms.flow.Delay;
import nl.tudelft.simulation.dsol.formalisms.flow.Generator;
import nl.tudelft.simulation.dsol.formalisms.flow.Release;
import nl.tudelft.simulation.dsol.formalisms.flow.Seize;
import nl.tudelft.simulation.dsol.formalisms.flow.StationInterface;
import nl.tudelft.simulation.dsol.formalisms.flow.statistics.Utilization;
import nl.tudelft.simulation.dsol.model.AbstractDSOLModel;
import nl.tudelft.simulation.dsol.simtime.dist.DistContinuousSimulationTime;
import nl.tudelft.simulation.dsol.simulators.DEVSSimulatorInterface;
import nl.tudelft.simulation.dsol.statistics.SimTally;
import nl.tudelft.simulation.jstats.distributions.DistConstant;
import nl.tudelft.simulation.jstats.distributions.DistDiscreteConstant;
import nl.tudelft.simulation.jstats.distributions.DistExponential;
import nl.tudelft.simulation.jstats.streams.MersenneTwister;
import nl.tudelft.simulation.jstats.streams.StreamInterface;

/**
 * The M/M/1 example as published in Simulation Modeling and Analysis by A.M.
 * Law &amp; W.D. Kelton section 1.4 and 2.4.
 * <p>
 * (c) copyright 2015-2020 <a href="http://www.simulation.tudelft.nl">Delft
 * University of Technology </a>, the Netherlands. <br>
 * See for project information
 * <a href="http://www.simulation.tudelft.nl">www.simulation.tudelft.nl </a>
 * <br>
 * License of use: <a href="http://www.gnu.org/copyleft/lesser.html">Lesser
 * General Public License (LGPL) </a>, no warranty.
 * 
 * @version 2.0 21.09.2003 <br>
 * @author <a href="https://www.linkedin.com/in/peterhmjacobs">Peter Jacobs </a>
 */
public class MM1Queue41Model extends AbstractDSOLModel<Double, DEVSSimulatorInterface<Double>> {
	/** The default serial version UID for serializable classes. */
	private static final long serialVersionUID = 1L;

	/** tally dN. */
	SimTally<Double> dN;

	/** tally qN. */
	SimTally<Double> qN;

	/** utilization uN. */
	Utilization<Double> uN;

	/** PARAMETER iat. */
	public double iat = Double.NaN;

	/** PARAMETER serviceTime. */
	public double serviceTime = Double.NaN;

	/** PARAMETER seed. */
	public long seed = 1;

	/**
	 * Construct the model.
	 * 
	 * @param simulator the simulator
	 */
	public MM1Queue41Model(final DEVSSimulatorInterface<Double> simulator) {
		super(simulator);
	}

	/** {@inheritDoc} */
	@Override
	public final void constructModel() throws SimRuntimeException {
		if (Double.isNaN(this.iat)) {
			throw new SimRuntimeException("Parameter iat not defined for model");
		}
		if (Double.isNaN(this.serviceTime)) {
			throw new SimRuntimeException("Parameter servicetime not defined for model");
		}

		StreamInterface defaultStream = new MersenneTwister(this.seed);

		// The Generator
		Generator<Double> generator = new Generator<Double>("generator", getSimulator(), Object.class, null);
		generator
				.setInterval(new DistContinuousSimulationTime.TimeDouble(new DistExponential(defaultStream, this.iat)));
		generator.setStartTime(new DistContinuousSimulationTime.TimeDouble(new DistConstant(defaultStream, 0.0)));
		generator.setBatchSize(new DistDiscreteConstant(defaultStream, 1));
		generator.setMaxNumber(1000);

		// The queue, the resource and the release
		Resource<Double> resource = new Resource<>(getSimulator(), "resource", 1.0);

		// created a resource
		StationInterface<Double> queue = new Seize<Double>("queue", getSimulator(), resource);
		StationInterface<Double> release = new Release<Double>("release", getSimulator(), resource, 1.0);

		// The server
		DistContinuousSimulationTime<Double> serviceTimeDistribution = new DistContinuousSimulationTime.TimeDouble(
				new DistExponential(defaultStream, this.serviceTime));
		StationInterface<Double> server = new Delay<Double>("delay", getSimulator(), serviceTimeDistribution);

		// The flow
		generator.setDestination(queue);
		queue.setDestination(server);
		server.setDestination(release);

		// Statistics
		try {
			this.dN = new SimTally<Double>("d(n)", getSimulator(), queue, Seize.DELAY_TIME);
			this.qN = new SimTally<Double>("q(n)", getSimulator(), queue, Seize.QUEUE_LENGTH_EVENT);
			this.uN = new Utilization<Double>("u(n)", getSimulator(), server);
		} catch (RemoteException exception) {
			exception.printStackTrace();
		}
	}

	/** {@inheritDoc} */
	@Override
	public Serializable getSourceId() {
		return "MM1Queue41Model";
	}

}
