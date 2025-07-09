import os
import pandas as pd
import numpy as np
import re
from sklearn.linear_model import LinearRegression
from sklearn.metrics import r2_score, mean_absolute_error, mean_squared_error
import matplotlib.pyplot as plt

def convert_flag(flag):
    return 1 if flag == 'A' else 0

def extract_map_size(filename):
    nums = re.findall(r'\d+', filename)
    if len(nums) >= 2:
        return int(nums[0]), int(nums[1])
    return np.nan, np.nan

def linear_regression_analysis_grouped(df, features, target='instructionCount', title='', output_file=None, output_folder='results/linear'):
    def print_out(*args, **kwargs):
        print(*args, **kwargs)
        if output_file:
            with open(output_file, 'a') as f:
                print(*args, **kwargs, file=f)

    print_out(f'==== Linear Regression Analysis: {title} ====')
    print_out(f'Target: {target}')
    print_out(f'Features: {features}')

    X = df[features].values
    y = df[target].values

    model = LinearRegression()
    model.fit(X, y)
    y_pred = model.predict(X)

    r2 = r2_score(y, y_pred)
    mae = mean_absolute_error(y, y_pred)
    mse = mean_squared_error(y, y_pred)

    coefs = ', '.join([f'{c:.4f}' for c in model.coef_])
    intercept = model.intercept_

    print_out(f'  Coefs: [{coefs}]')
    print_out(f'  Intercept: {intercept:.4f}')
    print_out(f'  R²: {r2:.4f}')
    print_out(f'  MAE: {mae:.2f}')
    print_out(f'  MSE: {mse:.2f}\n')

    # Plot --------------------------------------------------
    plt.figure(figsize=(10, 6))
    plt.scatter(range(len(y)), y, label='Actual')
    plt.scatter(range(len(y_pred)), y_pred, label='Predicted', marker='x')


    plt.suptitle(f'{title}: Multi-linear regression', fontweight='bold', fontsize=18, y=0.98)

    subtitle = (
        f'R²={r2:.4f} | MAE={mae:.2f} | MSE={mse:.2f}\n\n'
        f'Target={target} | Features={features}\n\n'
        f'Coefs=[{coefs}] | Intercept={intercept:.2f}'
    )
    plt.title(subtitle, fontsize=12, pad=15)

    plt.xlabel('Sample index')
    plt.ylabel('Instruction Count')
    plt.legend()
    plt.tight_layout(rect=[0, 0, 0.98, 1])

    os.makedirs(output_folder, exist_ok=True)
    safe_title = title.replace(' ', '_')
    plt.savefig(f'{output_folder}/{safe_title}_linear.png')
    plt.close()

output_log = 'linear.log'
open(output_log, 'w').close()

df_capture = pd.read_csv('samples/nists_csv/capturetheflag.csv')
df_capture['flag'] = df_capture['flag'].apply(convert_flag)
linear_regression_analysis_grouped(df_capture, ['gridSize', 'blue', 'red', 'flag'], 'instructionCount', 'Capture The Flag', output_log)

df_game = pd.read_csv('samples/nists_csv/gameoflife.csv')
df_game[['mapWidth', 'mapHeight']] = df_game['mapFile'].apply(lambda x: pd.Series(extract_map_size(x)))
linear_regression_analysis_grouped(df_game, ['iterations', 'mapWidth', 'mapHeight'], 'instructionCount', 'Game of Life', output_log)

df_fifteen = pd.read_csv('samples/nists_csv/fifteenpuzzle.csv')
linear_regression_analysis_grouped(df_fifteen, ['size', 'shuffles'], 'instructionCount', 'Fifteen Puzzle', output_log)
