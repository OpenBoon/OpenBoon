# This script selects and run other scripts placed in the scripts/ directory

import glob
import streamlit as st


def _max_width_():
    max_width_str = f"max-width: 2000px;"
    st.markdown(
        f"""
    <style>
    .reportview-container .main .block-container{{
        {max_width_str}
    }}
    </style>    
    """,
        unsafe_allow_html=True,
    )


# Make sure we always run in wide mode
_max_width_()

scripts = glob.glob('scripts/*.py')

st.sidebar.image('BoonAI-logo.png', width=200)
script = st.sidebar.selectbox('Script to run', scripts)
st.sidebar.markdown('---')
exec(open(script).read())
