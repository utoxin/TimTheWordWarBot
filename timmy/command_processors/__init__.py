from .utility.timer import Timer
from timmy.event_handlers import command_handler_instance
from .writing.wars import Wars


def register_processors():
    timer = Timer()
    timer.register_commands(command_handler_instance)

    wars = Wars()
    wars.register_commands(command_handler_instance)
