import { useState } from 'react'
import PropTypes from 'prop-types'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import DatasetDeleteModal from '../Dataset/DeleteModal'

const DatasetsMenu = ({ projectId, datasetId, name, setDatasetFields }) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)

  return (
    <>
      <Menu open="bottom-left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => {
                    onClick()
                    setDeleteModalOpen(true)
                  }}
                >
                  Delete
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>

      <DatasetDeleteModal
        projectId={projectId}
        datasetId={datasetId}
        name={name}
        isDeleteModalOpen={isDeleteModalOpen}
        setDeleteModalOpen={setDeleteModalOpen}
        setDatasetFields={setDatasetFields}
      />
    </>
  )
}

DatasetsMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  setDatasetFields: PropTypes.func.isRequired,
}

export default DatasetsMenu
