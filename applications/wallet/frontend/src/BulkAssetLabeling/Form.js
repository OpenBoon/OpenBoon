import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import AllSvg from '../Icons/all.svg'

import FlashMessageErrors from '../FlashMessage/Errors'
import Combobox from '../Combobox'
import RadioGroup from '../Radio/Group'

import { SCOPE_OPTIONS } from '../AssetLabeling/helpers'

const CLASSIFICATION = 'Classification'

const BulkAssetLabelingForm = ({ projectId, datasetType, state, dispatch }) => {
  const {
    data: { count, results: options },
  } = useSWR(
    `/api/v1/projects/${projectId}/datasets/${state.datasetId}/get_labels/`,
  )

  if (datasetType !== CLASSIFICATION) {
    return (
      <div
        css={{
          padding: spacing.normal,
          color: colors.structure.white,
          fontStyle: typography.style.italic,
          borderBottom: constants.borders.regular.smoke,
        }}
      >
        Bulk Labeling is only possible for &quot;Classification&quot; type
        datasets. Please select another dataset.
      </div>
    )
  }

  return (
    <div
      css={{ display: 'flex', flexDirection: 'column', flex: 1, height: '0%' }}
    >
      <div
        css={{
          padding: spacing.normal,
          fontWeight: typography.weight.bold,
        }}
      >
        Add Classification Label
      </div>

      <div
        css={{
          borderBottom: constants.borders.regular.smoke,
          backgroundColor: colors.structure.coal,
          overflow: 'auto',
        }}
      >
        <FlashMessageErrors
          errors={state.errors}
          styles={{
            padding: spacing.normal,
            paddingBottom: 0,
            div: { flex: 1 },
          }}
        />

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
          <AllSvg
            height={constants.bbox}
            css={{ color: colors.structure.iron, marginRight: spacing.normal }}
          />

          <div>
            <Combobox
              key={count}
              label="Label:"
              options={options}
              value={state.lastLabel}
              onChange={({ value }) => {
                dispatch({ lastLabel: value, lastScope: state.lastScope })
              }}
              hasError={state.errors.label !== undefined}
              errorMessage={state.errors.label}
            />

            <div css={{ height: spacing.base }} />

            <RadioGroup
              legend="Select Scope"
              options={SCOPE_OPTIONS.map((option) => ({
                ...option,
                legend: '',
                initialValue: state.lastScope === option.value,
              }))}
              onClick={({ value }) => {
                dispatch({ lastLabel: state.lastLabel, lastScope: value })
              }}
            />
          </div>
        </div>
      </div>
    </div>
  )
}

BulkAssetLabelingForm.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetType: PropTypes.string.isRequired,
  state: PropTypes.shape({
    datasetId: PropTypes.string.isRequired,
    lastLabel: PropTypes.string.isRequired,
    lastScope: PropTypes.string.isRequired,
    errors: PropTypes.shape({
      label: PropTypes.string,
    }).isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default BulkAssetLabelingForm
