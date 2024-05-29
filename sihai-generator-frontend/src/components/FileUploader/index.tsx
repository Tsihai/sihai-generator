import {uploadFileUsingPost,} from '@/services/backend/fileController';
import {InboxOutlined} from '@ant-design/icons';
import {message, Upload, UploadFile, UploadProps} from 'antd';
import React from 'react';

const {Dragger} = Upload;

interface Props {
  biz: string;
  onChange?: (FileList: UploadFile[]) => void;
  value?: UploadFile[];
  description?: string;
}

/**
 * 文件上传组件
 * @constructor
 */
const FileUploader: React.FC<Props> = (props) => {

  const {biz, onChange, value, description} = props;

  // 是否正在上传
  const [uploading, setUploading] = React.useState<boolean>(false);

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: false,
    listType: 'text',
    maxCount: 1,
    disabled: uploading,
    fileList: value,
    onChange({ fileList }) {
      onChange?.(fileList);
    },
    customRequest: async (fileObj: any) => {
      setUploading(true);
      try {
        const res = await uploadFileUsingPost({
          biz
        }, {}, fileObj.file);
        fileObj.onSuccess(res.data);
      } catch (e: any) {
        message.error('上传失败，' + e.message);
        fileObj.onError(e);
      }
      setUploading(false);
    },
  };

  return (
    <Dragger {...uploadProps}>
      <p className="ant-upload-drag-icon">
        <InboxOutlined/>
      </p>
      <p className="ant-upload-text">单击或拖动文件到此区域进行上传</p>
      <p className="ant-upload-hint">
        {description}
      </p>
    </Dragger>
  );
};

export default FileUploader;
