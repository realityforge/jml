package jamex.routes;

import jamex.link.MessageLink;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Extension of the default OSGi bundle activator
 */
public final class Activator
    implements BundleActivator
{
  private static final String DMQ_NAME = "DeadMessageQueue";
  private static final String CHANNEL_1_NAME = "CHANNEL_1";
  private static final String CHANNEL_2_NAME = "CHANNEL_2";
  private Session consumerSession;
  private MessageLink link;
  private Session producerSession;
  private Connection connection;

  /**
   * Called whenever the OSGi framework starts our bundle
   */
  public void start( BundleContext bc )
      throws Exception
  {
    final ServiceTracker tracker =
        new ServiceTracker( bc, ConnectionFactory.class.getName(), null );
    final ConnectionFactory factory =
        (ConnectionFactory)tracker.waitForService( 100 );
    connection = factory.createConnection();
    consumerSession = connection.createSession( false, Session.AUTO_ACKNOWLEDGE );
    subscribe( consumerSession );

    link = createLink();
    link.start( connection.createSession( false, Session.AUTO_ACKNOWLEDGE ) );

    producerSession = connection.createSession( false, Session.AUTO_ACKNOWLEDGE );
    for( int i = 0; i < 5; i++ )
    {
      publish( producerSession, getMessage() );
    }

    connection.start();
  }

  /**
   * Called whenever the OSGi framework stops our bundle
   */
  public void stop( BundleContext bc )
      throws Exception
  {
    if( null != link ) link.stop();
    if( null != consumerSession ) consumerSession.close();
    if( null != producerSession ) producerSession.close();
    connection.close();
  }

  private static String getMessage()
      throws Exception
  {
    return "Message - Time " + System.nanoTime();
  }

  private static MessageLink createLink()
      throws Exception
  {
    log( "createLink()" );
    final MessageLink link = new MessageLink();
    link.setDmqName( DMQ_NAME );
    link.setInputQueue( CHANNEL_1_NAME, null );
    link.setOutputQueue( CHANNEL_2_NAME );
    link.setName( "MrLink" );

    //link.setInputVerifier( MessageVerifier.newXSDVerifier( Main.class.getResource( "catalog.xsd" ) ) );
    //createLink.setOutputVerifier( MessageVerifier.newXSDVerifier( Main.class.getResource( "catalog.xsd" ) ) );
    // link.setTransformer( MessageTransformer.newXSLTransformer( Main.class.getResource( "transform.xsl" ) ) );
    return link;
  }

  private static void publish( final Session session, final String messageContent )
  {
    log( "publish()" );
    try
    {
      final Destination destination = session.createQueue( CHANNEL_1_NAME );
      final MessageProducer producer = session.createProducer( destination );
      final Message message = session.createTextMessage( messageContent );

      // Disable generation of ids as we don't care about them
      // (Actually ignored by OMQ)
      producer.setDisableMessageID( true );
      // Disable generation of approximate transmit timestamps as we don't care about them
      producer.setDisableMessageTimestamp( true );
      producer.setPriority( 1 );
      producer.setDeliveryMode( DeliveryMode.NON_PERSISTENT );
      producer.send( message );
      producer.close();
    }
    catch( final JMSException e )
    {
      e.printStackTrace();
    }
  }

  private static void subscribe( final Session session )
  {
    log( "subscribe()" );
    try
    {
      final Destination destination = session.createQueue( CHANNEL_2_NAME );
      final MessageConsumer consumer = session.createConsumer( destination );
      consumer.setMessageListener( new SimpleListener() );
    }
    catch( final JMSException e )
    {
      e.printStackTrace();
    }
  }

  static void log( final String message )
  {
    synchronized( System.out )
    {
      System.out.println( message );
    }
  }

  static void log( final Throwable t )
  {
    synchronized( System.out )
    {
      t.printStackTrace( System.out );
    }
  }

  static private class SimpleListener implements MessageListener
  {
    public void onMessage( final Message message )
    {
      try
      {
        log( "onMessage() received => " + message.getJMSMessageID() + "\n message => " + message );
      }
      catch( final JMSException e )
      {
        log( e );
        e.printStackTrace();
      }
      //throw new IllegalStateException( );
    }
  }
}
