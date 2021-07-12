import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, constants } from '../Styles'

import ButtonGroup from '../Button/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import DatasetConcepts from '../DatasetConcepts'

import ModelDatasetHeader from './Header'

const ModelDataset = ({ setErrors }) => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  const { data: model } = useSWR(
    `/api/v1/projects/${projectId}/models/${modelId}/`,
  )

  if (!model.datasetId) {
    return (
      <div
        css={{
          color: colors.structure.steel,
          maxWidth: constants.paragraph.maxWidth,
        }}
      >
        <i>
          Datasets are groups of labels added to assets that are used by the
          model for training. When using an uploaded, pre-trained model, only
          the testing labels in the dataset are used.
        </i>

        <ButtonGroup>
          <Link href={`/${projectId}/models/${modelId}/link`} passHref>
            <Button variant={BUTTON_VARIANTS.PRIMARY}>
              Link a Dataset to the Model
            </Button>
          </Link>
        </ButtonGroup>
      </div>
    )
  }

  return (
    <div css={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
      <ModelDatasetHeader
        projectId={projectId}
        modelId={modelId}
        model={model}
        setErrors={setErrors}
      />

      <DatasetConcepts
        projectId={projectId}
        datasetId={model.datasetId}
        actions={false}
      />
    </div>
  )
}

ModelDataset.propTypes = {
  setErrors: PropTypes.func.isRequired,
}

export default ModelDataset
