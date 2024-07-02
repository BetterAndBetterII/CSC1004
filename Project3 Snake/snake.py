import random
import tkinter as tk
from datetime import datetime, timedelta

BOARD_WIDTH = 20
BOARD_HEIGHT = 20
CELL_SIZE = 20
MAX_SPEED = 180
MIN_SPEED = 80
SPEEDUP_FACTOR = 10
SPEEDUP_PERIOD = 5


class Point:
    x: int
    y: int

    def __init__(self, pos_x, pos_y):
        self.x = pos_x
        self.y = pos_y

    def clone(self):
        return Point(self.x, self.y)


class Board:
    height: int
    width: int

    def __init__(self, height, width):
        self.height = height
        self.width = width

    def is_inside(self, point: Point):
        return 0 <= point.x < self.width and 0 <= point.y < self.height


class Food:
    x: int
    y: int
    color: str

    def __init__(self, pos_x, pos_y, color):
        self.x = pos_x
        self.y = pos_y
        self.color = color

    def effect(self, snake):
        pass


class SimpleFood(Food):
    def __init__(self, pos_x, pos_y):
        super().__init__(pos_x, pos_y, "red")

    def effect(self, snake):
        snake.grow()
        snake.score += 1


class SpeedUpFood(Food):
    def __init__(self, pos_x, pos_y):
        super().__init__(pos_x, pos_y, "purple")

    def effect(self, snake):
        snake.speedup()
        snake.score += 1


class SlowDownFood(Food):
    def __init__(self, pos_x, pos_y):
        super().__init__(pos_x, pos_y, "black")

    def effect(self, snake):
        snake.speedup(-10)
        snake.score += 1


class FoodGenerator:
    food_type_list: list
    board_width: int
    board_height: int

    def __init__(self, board_width, board_height, food_type_list: list):
        self.board_height = board_height
        self.board_width = board_width
        self.food_type_list = food_type_list

    def generate_food(self):
        random_type = random.randint(0, len(self.food_type_list) - 1)
        # print(f"Generated {self.food_type_list[random_type]}")
        return self.food_type_list[random_type](random.randint(0, self.board_width - 1),
                                                random.randint(0, self.board_height - 1))


class Snake:
    board: Board
    head: Point
    body: list[Point]
    direction: str
    speed: int
    start_time: datetime
    score: int

    def __init__(self, board: Board, initial_x, initial_y, initial_length=3, initial_speed=100):
        self.next_speed_up_time = None
        self.last_direction = None
        self.board = board
        self.head = Point(initial_x, initial_y)
        self.body = []
        self.speed = initial_speed
        self.start_time = datetime.now()
        self.score = 0
        for i in range(initial_length):
            self.body.append(Point(initial_x, initial_y + i))
        self.direction = "up"

    def move(self):
        self.natural_speed_up()
        is_moved = False
        if self.direction == "up":
            if self.board.is_inside(Point(self.head.x, self.head.y - 1)):
                self.head.y -= 1
                is_moved = True
        elif self.direction == "down":
            if self.board.is_inside(Point(self.head.x, self.head.y + 1)):
                self.head.y += 1
                is_moved = True
        elif self.direction == "left":
            if self.board.is_inside(Point(self.head.x - 1, self.head.y)):
                self.head.x -= 1
                is_moved = True
        elif self.direction == "right":
            if self.board.is_inside(Point(self.head.x + 1, self.head.y)):
                self.head.x += 1
                is_moved = True
        # self.print_body_coords()
        if is_moved:
            self.body.insert(0, self.head.clone())
            self.body.pop()
            self.last_direction = self.direction

    def change_direction(self, new_direction):
        if self.last_direction == "up" and new_direction == "down":
            return
        if self.last_direction == "down" and new_direction == "up":
            return
        if self.last_direction == "left" and new_direction == "right":
            return
        if self.last_direction == "right" and new_direction == "left":
            return
        if new_direction == "up" and self.direction != "down":
            self.direction = new_direction
        elif new_direction == "down" and self.direction != "up":
            self.direction = new_direction
        elif new_direction == "left" and self.direction != "right":
            self.direction = new_direction
        elif new_direction == "right" and self.direction != "left":
            self.direction = new_direction

    def grow(self):
        # print("Growing!")
        self.body.append(self.body[-1])

    def check_food_collision(self, food: Food):
        if self.head.x == food.x and self.head.y == food.y:
            return True
        for point in self.body:
            if food.x == point.x and food.y == point.y:
                return True
        return False

    def check_body_collision(self):
        for point in self.body[1:]:
            if self.head.x == point.x and self.head.y == point.y:
                return True
        return False

    def check_wall_collision(self):
        if self.direction == "up" and self.head.y == 0:
            return True
        if self.direction == "down" and self.head.y == self.board.height - 1:
            return True
        if self.direction == "left" and self.head.x == 0:
            return True
        if self.direction == "right" and self.head.x == self.board.width - 1:
            return True
        return False

    def get_head(self):
        return self.head

    def get_body(self):
        return self.body

    def get_direction(self):
        return self.direction

    def get_length(self):
        return len(self.body)

    def print_body_coords(self):
        print(f"Head: ({self.head.x}, {self.head.y})  Body: ", end="")
        for point in self.body:
            print(f"({point.x}, {point.y})", end=" ")
        print()

    def speedup(self, delta=10):
        if self.speed + delta > MAX_SPEED or self.speed + delta < MIN_SPEED:
            return
        # print("Speedup!")
        self.speed += delta

    def get_speed(self):
        return 200 - self.speed

    def natural_speed_up(self):
        if not self.next_speed_up_time:
            self.next_speed_up_time = self.start_time + timedelta(seconds=SPEEDUP_PERIOD)
            return
        if datetime.now() > self.next_speed_up_time:
            self.speedup(SPEEDUP_FACTOR)
            self.next_speed_up_time = datetime.now() + timedelta(seconds=SPEEDUP_PERIOD)


class SnakeGame:
    def __init__(self, master, board_width=20, board_height=20, cell_size=20):
        self.master = master
        self.board_width = board_width
        self.board_height = board_height
        self.cell_size = cell_size
        self.board = Board(board_height, board_width)
        self.snake = Snake(self.board, board_width // 2, board_height // 2)
        self.food_generator = FoodGenerator(board_width, board_height, [SimpleFood, SpeedUpFood, SlowDownFood])
        self.food = self.food_generator.generate_food()

        self.canvas = tk.Canvas(master, width=self.board_width * self.cell_size,
                                height=self.board_height * self.cell_size, bg='white')
        self.canvas.pack()

        self.status_frame = tk.Frame(master)
        self.status_frame.pack(side=tk.RIGHT, fill=tk.Y)
        self.score_label = tk.Label(self.status_frame, text="Score: 0", font=("Helvetica", 16))
        self.score_label.pack()
        self.speed_label = tk.Label(self.status_frame, text=f"Speed: {self.snake.get_speed()} ms",
                                    font=("Helvetica", 16))
        self.speed_label.pack()

        # Menu
        self.menu_bar = tk.Menu(master)
        master.config(menu=self.menu_bar)

        game_menu = tk.Menu(self.menu_bar, tearoff=0)
        self.menu_bar.add_cascade(label="Game", menu=game_menu)
        game_menu.add_command(label="Restart", command=self.restart_game)  # Restart

        self.start_game()
        
    def start_game(self):
        self.canvas.create_text(self.board_width * self.cell_size // 2, self.board_height * self.cell_size // 2,
                                text="Welcome!", font=("Helvetica", 32), fill="red")
        self.canvas.create_text(self.board_width * self.cell_size // 2,
                                self.board_height * self.cell_size // 2 + 50,
                                text="Press <Space> to Start", font=("Helvetica", 16), fill="red")
        self.master.unbind("<KeyPress>")
        # Bind Space key to restart game
        self.master.bind("<space>", lambda event: self.restart_game())

    def draw(self):
        self.canvas.delete(tk.ALL)
        self.canvas.create_rectangle(self.food.x * self.cell_size, self.food.y * self.cell_size,
                                     (self.food.x + 1) * self.cell_size, (self.food.y + 1) * self.cell_size,
                                     fill=self.food.color)

        head = self.snake.get_head()
        self.canvas.create_rectangle(head.x * self.cell_size, head.y * self.cell_size,
                                     (head.x + 1) * self.cell_size, (head.y + 1) * self.cell_size,
                                     fill="blue")
        for point in self.snake.get_body()[1:]:
            self.canvas.create_rectangle(point.x * self.cell_size, point.y * self.cell_size,
                                         (point.x + 1) * self.cell_size, (point.y + 1) * self.cell_size,
                                         fill="green")

    def update_game(self):
        if self.snake.check_body_collision() or self.snake.check_wall_collision():
            self.game_over()
            return
        self.snake.move()
        if self.snake.check_food_collision(self.food):
            self.food.effect(self.snake)
            self.food = self.food_generator.generate_food()
        self.score_label.config(text=f"Score: {self.snake.score}")
        self.speed_label.config(text=f"Speed: {self.snake.speed}")
        self.draw()
        self.master.after(self.snake.get_speed(), self.update_game)

    def on_key_press(self, event):
        new_direction = {
            "Up": "up",
            "Down": "down",
            "Left": "left",
            "Right": "right"
        }.get(event.keysym, self.snake.direction)
        self.snake.change_direction(new_direction)

    def game_over(self):
        self.canvas.create_text(self.board_width * self.cell_size // 2, self.board_height * self.cell_size // 2,
                                text="Game Over", font=("Helvetica", 32), fill="red")
        self.canvas.create_text(self.board_width * self.cell_size // 2, self.board_height * self.cell_size // 2 + 50,
                                text="Press <Space> to Restart", font=("Helvetica", 16), fill="red")
        self.master.unbind("<KeyPress>")
        # Bind Space key to restart game
        self.master.bind("<space>", lambda event: self.restart_game())

    def restart_game(self):
        self.master.unbind("<space>")
        self.snake = Snake(self.board, self.board_width // 2, self.board_height // 2)
        self.food = self.food_generator.generate_food()
        self.master.bind("<KeyPress>", self.on_key_press)
        self.update_game()


def main():
    root = tk.Tk()
    root.title("Snake Game")
    SnakeGame(root, BOARD_WIDTH, BOARD_HEIGHT, CELL_SIZE)
    root.mainloop()


if __name__ == "__main__":
    main()
