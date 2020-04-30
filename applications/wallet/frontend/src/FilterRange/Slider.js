/* eslint-disable react/jsx-props-no-spreading */

import PropTypes from 'prop-types'
import { Slider, Rail, Handles, Tracks } from 'react-compound-slider'

import { colors } from '../Styles'

const RAIL_HEIGHT = 2
const TRACK_HEIGHT = 4
const HANDLE_WIDTH = 8
const HANDLE_HEIGHT = 24

const FilterRangeSlider = ({ domain, values, setValues, onChange }) => {
  return (
    <Slider
      mode={2}
      step={0.1}
      domain={domain}
      rootStyle={{ position: 'relative', width: '100%' }}
      onUpdate={setValues}
      onChange={onChange}
      values={values}
    >
      <Rail>
        {({ getRailProps }) => (
          <>
            <div
              css={{
                position: 'absolute',
                width: '100%',
                height: RAIL_HEIGHT,
                transform: 'translate(0%, -50%)',
                cursor: 'pointer',
              }}
              {...getRailProps()}
            />
            <div
              css={{
                position: 'absolute',
                width: '100%',
                height: RAIL_HEIGHT,
                transform: 'translate(0%, -50%)',
                pointerEvents: 'none',
                backgroundColor: colors.structure.iron,
              }}
            />
          </>
        )}
      </Rail>
      <Handles>
        {({ handles, getHandleProps }) => (
          <div>
            {handles.map(({ id, value, percent }) => (
              <button
                key={id}
                type="button"
                role="slider"
                aria-valuemin={domain.min}
                aria-valuemax={domain.max}
                aria-valuenow={value}
                style={{
                  padding: 0,
                  margin: 0,
                  border: 'none',
                  left: `${percent}%`,
                  position: 'absolute',
                  transform: 'translate(-50%, -50%)',
                  zIndex: 2,
                  width: HANDLE_WIDTH,
                  height: HANDLE_HEIGHT,
                  backgroundColor: colors.structure.steel,
                  borderRadius: 1,
                  cursor: 'pointer',
                }}
                {...getHandleProps(id)}
              />
            ))}
          </div>
        )}
      </Handles>
      <Tracks left={false} right={false}>
        {({ tracks, getTrackProps }) => (
          <div>
            {tracks.map(({ id, source, target }) => (
              <div
                key={id}
                style={{
                  position: 'absolute',
                  transform: 'translate(0%, -50%)',
                  height: TRACK_HEIGHT,
                  zIndex: 1,
                  backgroundColor: colors.key.one,
                  cursor: 'pointer',
                  left: `${source.percent}%`,
                  width: `${target.percent - source.percent}%`,
                }}
                {...getTrackProps()}
              />
            ))}
          </div>
        )}
      </Tracks>
    </Slider>
  )
}

FilterRangeSlider.propTypes = {
  domain: PropTypes.arrayOf(PropTypes.number).isRequired,
  values: PropTypes.arrayOf(PropTypes.number).isRequired,
  setValues: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,
}

export default FilterRangeSlider
