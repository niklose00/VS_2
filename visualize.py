import pandas as pd
import matplotlib.pyplot as plt
import sys

def main(path="dsm_log.csv"):
    df = pd.read_csv(path)
    nodes = sorted(set(df['observer']))
    fig, axes = plt.subplots(len(nodes), 1, figsize=(8, 4*len(nodes)), sharex=True)
    if len(nodes) == 1:
        axes = [axes]
    for ax, obs in zip(axes, nodes):
        subset = df[df['observer'] == obs]
        for target in sorted(set(subset['observed'])):
            tdata = subset[subset['observed'] == target]
            ax.plot(tdata['tick'], tdata['value'], label=f"{obs}->{target}")
            bj = tdata[tdata['type'] == 'backjump']
            ax.scatter(bj['tick'], bj['value'], color='red', marker='x', label='backjump')
            mu = tdata[tdata['type'] == 'missing_update']
            ax.scatter(mu['tick'], mu['value'], color='orange', marker='o', label='missing_update')
        ax.set_title(f"Observations by {obs}")
        ax.set_ylabel("value")
        ax.legend()
    axes[-1].set_xlabel("tick")
    plt.tight_layout()
    plt.show()

if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "dsm_log.csv")
