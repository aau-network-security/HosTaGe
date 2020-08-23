import asyncio
import aio_pika

class AMQP(object):

    def  __init__(self,ip):
        self.ip = ip

    async def init(self,loop):
        connection = await aio_pika.connect_robust(
            "amqp://guest:guest@"+self.ip+"/", loop=loop
        )

        routing_key = "test_queue"

        channel = await connection.channel()    # type: aio_pika.Channel

        await channel.default_exchange.publish(
            aio_pika.Message(
                body='Hello {}'.format(routing_key).encode()
            ),
            routing_key=routing_key
        )

        await connection.close()

    def start(self):
        loop = asyncio.get_event_loop()
        try:
            loop.run_until_complete(self.init(loop))
        except Exception as e:
            print(e)
        loop.close()