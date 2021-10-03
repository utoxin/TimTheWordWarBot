import math
import re

from timmy.command_processors.base_command import BaseCommand
from timmy.data.command_data import CommandData
from timmy.db_access import boxodoom_db


class BoxODoomCommand(BaseCommand):
    user_commands = {"boxodoom"}

    def process(self, command_data: CommandData) -> None:
        if command_data.arg_count != 2:
            self.respond_to_user(command_data, "Usage: !boxodoom <difficulty> <duration in minutes>")
            self.respond_to_user(command_data, "Difficulty Options: extraeasy, easy, average, hard, extreme, insane, "
                                               "impossible, tadiera")
            return
        else:
            difficulty_pattern = re.compile(
                    '^((extra|super)?easy)|average|medium|normal|hard|extreme|insane|'
                    'impossible|tadiera$', re.IGNORECASE
            )

            arg1check = difficulty_pattern.match(command_data.args[0])
            arg2check = difficulty_pattern.match(command_data.args[1])

            difficulty = "normal"

            try:
                if arg1check:
                    difficulty = command_data.args[0]
                    duration = float(command_data.args[1])
                elif arg2check:
                    difficulty = command_data.args[1]
                    duration = float(command_data.args[0])
                else:
                    self.respond_to_user(command_data, "Difficulty must be one of: extraeasy, easy, average, hard, "
                                                       "extreme, insane, impossible, tadiera")
                    return
            except TypeError:
                duration = 0

            if duration == 0:
                self.respond_to_user(command_data, "Duration must be greater than 0.")
                return

            original_difficulty = difficulty

            if difficulty == "extraeasy" or difficulty == "supereasy":
                difficulty = "easy"

            if difficulty == "medium" or difficulty == "normal":
                difficulty = "average"

            if difficulty == "extreme" or difficulty == "insane" or difficulty == "impossible" \
                    or difficulty == "tadiera":
                difficulty = "hard"

            challenge = boxodoom_db.get_random_challenge(difficulty)

            if original_difficulty == 'extraeasy' or original_difficulty == 'supereasy':
                challenge *= 0.65
            elif original_difficulty == 'extreme':
                challenge *= 1.4
            elif original_difficulty == 'insane':
                challenge *= 1.8
            elif original_difficulty == 'impossible':
                challenge *= 2.2
            elif original_difficulty == 'tadiera':
                challenge *= 3

            modifier = 1.0 / math.log(duration + 1.0) / 1.5 + 0.68

            goal = round(duration * challenge * modifier)

            self.respond_to_user(command_data, f"Your goal is: {goal:,} in {duration} minutes.")
