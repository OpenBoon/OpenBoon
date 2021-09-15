import PropTypes from 'prop-types'
import useSWR from 'swr'

import { spacing, constants, colors, typography } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Combobox from '../Combobox'
import RadioGroup from '../Radio/Group'

import TrashSvg from '../Icons/trash.svg'

import { SCOPE_OPTIONS } from './helpers'

const AssetLabelingLabel = ({
  projectId,
  datasetId,
  state,
  dispatch,
  label: { label, scope, bbox, b64Image, simhash },
  onDelete,
}) => {
  const {
    data: { count, results: options },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/${datasetId}/get_labels/`)

  if (label) {
    return (
      <div
        css={{
          display: 'flex',
          padding: spacing.normal,
          borderBottom: constants.borders.regular.smoke,
          '&:last-of-type': {
            borderBottom: 'none',
          },
          ':hover': {
            backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
            svg: { opacity: 1 },
          },
        }}
      >
        <img
          css={{
            height: constants.bbox,
            width: constants.bbox,
            objectFit: 'contain',
            marginRight: spacing.normal,
          }}
          alt={bbox}
          title={bbox}
          src={b64Image}
        />

        <div
          css={{ flex: 1, display: 'flex', justifyContent: 'space-between' }}
        >
          <div>
            <div css={{ color: colors.structure.zinc }}>Label:</div>
            <div css={{ fontWeight: typography.weight.medium }}>{label}</div>
          </div>

          <div css={{ display: 'flex', alignItems: 'flex-start' }}>
            <div>
              <div css={{ color: colors.structure.zinc }}>Scope:</div>
              <div
                css={{
                  fontWeight: typography.weight.medium,
                  textTransform: 'capitalize',
                }}
              >
                {scope.toLowerCase()}
              </div>
            </div>

            <Button
              title="Remove Label"
              aria-label="Remove Label"
              variant={BUTTON_VARIANTS.ICON}
              onClick={() => {
                onDelete({ label: { label, scope, bbox, simhash } })
              }}
              style={{ marginTop: -spacing.small }}
            >
              <TrashSvg height={constants.icons.regular} css={{ opacity: 0 }} />
            </Button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div
      css={{
        display: 'flex',
        padding: spacing.normal,
        borderBottom: constants.borders.regular.smoke,
        '&:last-of-type': {
          borderBottom: 'none',
        },
      }}
    >
      <img
        css={{
          height: constants.bbox,
          width: constants.bbox,
          objectFit: 'contain',
          marginRight: spacing.normal,
        }}
        alt={bbox}
        title={bbox}
        src={b64Image}
      />

      <div>
        <Combobox
          key={count}
          label="Label:"
          options={options}
          value={state.label}
          onChange={({ value }) => {
            dispatch({ label: value, scope: state.scope, simhash })
          }}
          hasError={state.error !== undefined}
          errorMessage={state.error}
        />

        <div css={{ height: spacing.base }} />

        <RadioGroup
          legend="Select Scope"
          options={SCOPE_OPTIONS.map((option) => ({
            ...option,
            legend: '',
            initialValue: state.scope === option.value,
          }))}
          onClick={({ value }) => {
            dispatch({ label: state.label, scope: value, simhash })
          }}
        />
      </div>
    </div>
  )
}

AssetLabelingLabel.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  state: PropTypes.shape({
    label: PropTypes.string.isRequired,
    scope: PropTypes.string.isRequired,
    error: PropTypes.string,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
  label: PropTypes.shape({
    label: PropTypes.string.isRequired,
    scope: PropTypes.string.isRequired,
    bbox: PropTypes.arrayOf(PropTypes.number),
    b64Image: PropTypes.string.isRequired,
    simhash: PropTypes.string,
  }).isRequired,
  onDelete: PropTypes.func.isRequired,
}

export default AssetLabelingLabel
