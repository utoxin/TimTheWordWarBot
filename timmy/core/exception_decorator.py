def exception(logger):
    """
    A decorator that wraps the passed in function and logs
    exceptions should one occur
    """

    def decorator(func):
        def wrapper(*args, **kwargs):
            try:
                return func(*args, **kwargs)
            except:
                # log the exception
                err = "There was an exception in  "
                err += func.__name__
                logger.exception(err)
                # re-raise the exception
                raise

        return wrapper

    return decorator
