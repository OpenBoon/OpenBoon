import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, spacing, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from './helpers'

const FiltersReset = ({
  projectId,
  assetId,
  filters,
  filter,
  filterIndex,
  onReset,
}) => {
  const { data: fields } = useSWR(
    `/api/v1/projects/${projectId}/searches/fields/`,
  )

  const options = filter.attribute
    .split('.')
    .reduce((p, c) => p && p[c], fields)

  return (
    <div
      css={{
        display: 'flex',
        paddingTop: spacing.base,
        paddingBottom: spacing.base,
      }}
    >
      {options.length > 1 && (
        <select
          name="sources"
          id="source-selection"
          defaultValue={filter.type}
          onChange={({ target: { value } }) => {
            onReset()
            dispatch({
              action: ACTIONS.UPDATE_FILTER,
              payload: {
                projectId,
                assetId,
                filters,
                updatedFilter: { ...filter, type: value, values: {} },
                filterIndex,
              },
            })
          }}
          css={{
            backgroundColor: colors.transparent,
            border: 'none',
            fontSize: typography.size.regular,
            lineHeight: typography.height.regular,
            MozAppearance: 'none',
            WebkitAppearance: 'none',
            backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2IzYjNiMyIgLz4KPC9zdmc+')`,
            backgroundRepeat: `no-repeat, repeat`,
            backgroundPosition: `right top 50%`,
            paddingRight: spacing.comfy,
            color: colors.structure.zinc,
            fontFamily: 'Roboto Condensed',
            textTransform: 'uppercase',
            cursor: 'pointer',
            ':hover': {
              color: colors.structure.white,
              backgroundImage: `url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+CiAgPHBhdGggZD0iTTE0LjI0MyA3LjU4NkwxMCAxMS44MjggNS43NTcgNy41ODYgNC4zNDMgOSAxMCAxNC42NTcgMTUuNjU3IDlsLTEuNDE0LTEuNDE0eiIgZmlsbD0iI2ZmZmZmZiIgLz4KPC9zdmc+')`,
            },
          }}
        >
          {options.map((option) => {
            return (
              <option key={option} value={option}>
                {option}
              </option>
            )
          })}
        </select>
      )}

      <div css={{ flex: 1 }} />

      {filter.type !== 'exists' && (
        <Button
          style={{
            width: '100%',
            color: colors.structure.zinc,
            fontFamily: 'Roboto Condensed',
            textTransform: 'uppercase',
            ':hover': {
              color: colors.structure.white,
            },
          }}
          variant={VARIANTS.NEUTRAL}
          onClick={() => {
            onReset()
            dispatch({
              action: ACTIONS.UPDATE_FILTER,
              payload: {
                projectId,
                assetId,
                filters,
                updatedFilter: { ...filter, values: {} },
                filterIndex,
              },
            })
          }}
        >
          Reset
        </Button>
      )}
    </div>
  )
}

FiltersReset.propTypes = {
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
    type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
    attribute: PropTypes.string.isRequired,
    values: PropTypes.shape({ exists: PropTypes.bool }),
  }).isRequired,
  filterIndex: PropTypes.number.isRequired,
  onReset: PropTypes.func.isRequired,
}

export default FiltersReset
