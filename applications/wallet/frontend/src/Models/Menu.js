import { useState } from 'react'
import PropTypes from 'prop-types'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import ModelDeleteModal from '../Model/DeleteModal'

const ModelsMenu = ({ projectId, modelId }) => {
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

      <ModelDeleteModal
        projectId={projectId}
        modelId={modelId}
        isDeleteModalOpen={isDeleteModalOpen}
        setDeleteModalOpen={setDeleteModalOpen}
      />
    </>
  )
}

ModelsMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
}

export default ModelsMenu
