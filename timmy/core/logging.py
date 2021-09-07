import logging
import sys


def setup_console_logger():
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(logging.DEBUG)
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    handler.setFormatter(formatter)

    logging.basicConfig(level = logging.DEBUG, handlers = [handler])


def get_exception_logger():
    """
    Creates a logging object and returns it
    """
    logger = logging.getLogger("exception_logger")
    logger.setLevel(logging.INFO)
    # create the logging file handler
    fh = logging.FileHandler("exceptions.log")
    fmt = '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    formatter = logging.Formatter(fmt)
    fh.setFormatter(formatter)
    # add handler to logger object
    logger.addHandler(fh)
    return logger
