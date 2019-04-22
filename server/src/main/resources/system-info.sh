#!/bin/bash

#Get GPU information
if [[ "$OSTYPE" == "linux-gnu" ]]; then
    GPU=$(lspci | grep VGA | cut -d ":" -f3)

    GPU_NAME=$(lspci | grep VGA | cut -d ":" -f3 | cut -d "[" -f 2 | cut -d "]" -f 1);
    if [[ ${GPU} == *"NVIDIA"* ]];then
        GPU_TEMP=`nvidia-smi -q | grep "GPU Current Temp" | cut -d":" -f 2 | xargs`
        GPU_SHUTDOWN=`nvidia-smi -q | grep "GPU Shutdown Temp" | cut -d":" -f 2 | xargs`
        GPU_USE=`nvidia-smi --query-gpu=utilization.gpu --format=csv | tr '\n' ' '`
        GPU_USE=${GPU_USE/'utilization.gpu [%]'}
    fi

    echo "gpu_name: $GPU_NAME"
    echo "gpu_usage: $GPU_USE"
    echo "gpu_temp: $GPU_TEMP"
    echo "gpu_shutdown: $GPU_SHUTDOWN"
elif [[ "$OSTYPE" == "darwin"* ]]; then
        echo "OS: $OSTYPE"
elif [[ "$OSTYPE" == "cygwin" ]]; then
        echo "OS: $OSTYPE"
elif [[ "$OSTYPE" == "msys" ]]; then
        echo "OS: $OSTYPE"
elif [[ "$OSTYPE" == "win32" ]]; then
        echo "OS: $OSTYPE"
elif [[ "$OSTYPE" == "freebsd"* ]]; then
        echo "OS: $OSTYPE"
else
        echo "OS: $OSTYPE"
fi