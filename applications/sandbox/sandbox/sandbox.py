# This script selects and run other scripts placed in the scripts/ directory

import glob
import streamlit as st

scripts = glob.glob('scripts/*.py')

script = st.sidebar.selectbox('Script to run', scripts)
st.sidebar.markdown('---')
exec(open(script).read())
