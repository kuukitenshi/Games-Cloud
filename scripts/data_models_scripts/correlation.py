import os
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from scipy.stats import pearsonr

folder = "samples/bbl_csv/"
save_folder = f"results/correlations/"

os.makedirs(save_folder, exist_ok=True)

ctf_df = pd.read_csv(f'{folder}capturetheflag.csv')
ctf_df["scenario"] = ctf_df.apply(lambda row: f"Grid{row['gridSize']}_B{row['blue']}_R{row['red']}_Flag{row['flag']}", axis=1)

puzzle_15_df = pd.read_csv(f'{folder}fifteenpuzzle.csv')
puzzle_15_df["scenario"] = puzzle_15_df.apply(lambda row: f"Size{row['size']}_Shuffles{row['shuffles']}", axis=1)

game_of_life_df = pd.read_csv(f'{folder}gameoflife.csv')
game_of_life_df["scenario"] = game_of_life_df.apply(lambda row: f"{row['mapFile']}_Iter{row['iterations']}", axis=1)


def plot_correlation(df, title, filename):
    corr, _ = pearsonr(df["instructionCount"], df["bblCount"])
    print(f"{title} - Pearson correlation: {corr:.4f}")

    plt.figure(figsize=(8,6))
    sns.scatterplot(data=df, x="instructionCount", y="bblCount", s=100)
    plt.suptitle(title, fontweight='bold', fontsize=16)     
    plt.title(f"Pearson correlation = {corr:.4f}", fontsize=12)
    plt.xlabel("Instruction Count")
    plt.ylabel("Basic Block Count")
    plt.grid(True)
    plt.tight_layout(rect=[0, 0, 1, 1])
    plt.savefig(filename)
    plt.clf()           


plot_correlation(ctf_df, "Capture the Flag", f"{save_folder}ctf_corr.png")
plot_correlation(puzzle_15_df, "Fifteen Puzzles", f"{save_folder}puzz_corr.png")
plot_correlation(game_of_life_df, "Game of Life", f"{save_folder}gof_corr.png")
