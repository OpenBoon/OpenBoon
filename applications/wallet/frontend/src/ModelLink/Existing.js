import PropTypes from 'prop-types'
import useSWR from 'swr'

import { spacing, colors } from '../Styles'

import SectionTitle from '../SectionTitle'
import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onExistingLink } from './helpers'

const ModelLinkExisting = ({ projectId, model, state, dispatch }) => {
  const {
    data: { results: datasets },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/`)

  return (
    <div>
      <SectionTitle>Select Existing Dataset</SectionTitle>

      <div css={{ height: spacing.normal }} />

      <Select
        label="Dataset"
        defaultValue={state.datasetId}
        options={datasets
          .filter(({ type }) => type === model.datasetType)
          .map(({ id, name }) => ({ value: id, label: name }))}
        onChange={({ value: datasetId }) => {
          dispatch({ datasetId })
        }}
        variant={SELECT_VARIANTS.COLUMN}
        isRequired
        style={{ backgroundColor: colors.structure.smoke }}
      />

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() =>
            onExistingLink({
              projectId,
              model,
              datasetId: state.datasetId,
              dispatch,
            })
          }
          isDisabled={!state.datasetId || state.isLoading}
        >
          {state.isLoading ? 'Linking...' : 'Link Dataset'}
        </Button>
      </ButtonGroup>
    </div>
  )
}

ModelLinkExisting.propTypes = {
  projectId: PropTypes.string.isRequired,
  model: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    datasetId: PropTypes.string,
    runningJobId: PropTypes.string.isRequired,
    state: PropTypes.string.isRequired,
    datasetType: PropTypes.string.isRequired,
  }).isRequired,
  state: PropTypes.shape({
    datasetId: PropTypes.string.isRequired,
    isLoading: PropTypes.bool.isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default ModelLinkExisting
