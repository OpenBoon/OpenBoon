/* eslint-disable react/jsx-props-no-spreading */

import PropTypes from 'prop-types'
import { Slider, Rail, Handles, Tracks } from 'react-compound-slider'

import { colors } from '../Styles'

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
                height: 2,
                transform: 'translate(0%, -50%)',
                borderRadius: 7,
                cursor: 'pointer',
              }}
              {...getRailProps()}
            />
            <div
              css={{
                position: 'absolute',
                width: '100%',
                height: 2,
                transform: 'translate(0%, -50%)',
                borderRadius: 7,
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
                  width: 8,
                  height: 24,
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
                  height: 4,
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
