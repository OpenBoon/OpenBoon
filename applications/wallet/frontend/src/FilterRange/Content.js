import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'
import { bytesToSize } from '../Bytes/helpers'

import { colors, constants, spacing } from '../Styles'

import { dispatch, ACTIONS } from '../Filters/helpers'
import FiltersReset from '../Filters/Reset'

import FilterRangeSlider from './Slider'

const FilterRange = ({
  projectId,
  assetId,
  filters,
  filter: { type, attribute },
  filterIndex,
}) => {
  const encodedFilter = btoa(JSON.stringify({ type, attribute }))

  const {
    data: { results },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/aggregate/?filter=${encodedFilter}`,
  )

  const domain = [results.min, results.max]

  const [values, setValues] = useState(domain)

  return (
    <div>
      <FiltersReset
        payload={{
          projectId,
          assetId,
          filters,
          updatedFilter: {
            type,
            attribute,
            values: {},
          },
          filterIndex,
        }}
        onReset={() => setValues(domain)}
      />
      <div css={{ padding: spacing.normal }}>
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            paddingBottom: spacing.moderate,
          }}
        >
          <span>{bytesToSize({ bytes: results.min })}</span>
          <span>{bytesToSize({ bytes: results.max })}</span>
        </div>
        <div css={{ padding: spacing.small }}>
          <FilterRangeSlider
            domain={domain}
            values={values}
            setValues={(value) => setValues(value)}
            onChange={(value) =>
              dispatch({
                action: ACTIONS.UPDATE_FILTER,
                payload: {
                  projectId,
                  assetId,
                  filters,
                  updatedFilter: {
                    type,
                    attribute,
                    values: value,
                  },
                  filterIndex,
                },
              })
            }
          />
        </div>
        <div
          css={{
            paddingTop: spacing.comfy,
            color: colors.structure.zinc,
            display: 'flex',
            justifyContent: 'space-around',
          }}
        >
          <div css={{ display: 'flex', alignItems: 'center' }}>
            MIN &nbsp;
            <div
              css={{
                backgroundColor: colors.structure.lead,
                paddingTop: spacing.normal,
                paddingBottom: spacing.normal,
                width: 76,
                textAlign: 'center',
                borderRadius: constants.borderRadius.small,
              }}
            >
              {bytesToSize({ bytes: values[0] })}
            </div>
          </div>
          <div css={{ display: 'flex', alignItems: 'center' }}>
            MAX &nbsp;
            <div
              css={{
                backgroundColor: colors.structure.lead,
                paddingTop: spacing.normal,
                paddingBottom: spacing.normal,
                width: 76,
                textAlign: 'center',
                borderRadius: constants.borderRadius.small,
              }}
            >
              {bytesToSize({ bytes: values[1] })}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

FilterRange.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(
    PropTypes.shape({
      type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
      attribute: PropTypes.string,
      values: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    }).isRequired,
  ).isRequired,
  filter: PropTypes.shape({
    type: PropTypes.oneOf(['range']).isRequired,
    attribute: PropTypes.string.isRequired,
    values: PropTypes.shape({ exists: PropTypes.bool }),
  }).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterRange
